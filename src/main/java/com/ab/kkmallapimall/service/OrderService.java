package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.config.OrderTimeoutProperties;
import com.ab.kkmallapimall.dto.request.CreateOrderRequest;
import com.ab.kkmallapimall.dto.response.OrderItemVO;
import com.ab.kkmallapimall.dto.response.OrderVO;
import com.ab.kkmallapimall.entity.Address;
import com.ab.kkmallapimall.entity.Cart;
import com.ab.kkmallapimall.entity.Coupon;
import com.ab.kkmallapimall.entity.MallUser;
import com.ab.kkmallapimall.entity.Order;
import com.ab.kkmallapimall.entity.OrderItem;
import com.ab.kkmallapimall.entity.Product;
import com.ab.kkmallapimall.entity.UserCoupon;
import com.ab.kkmallapimall.event.OrderCreatedEvent;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.AddressMapper;
import com.ab.kkmallapimall.mapper.CartMapper;
import com.ab.kkmallapimall.mapper.CouponMapper;
import com.ab.kkmallapimall.mapper.MallUserMapper;
import com.ab.kkmallapimall.mapper.OrderItemMapper;
import com.ab.kkmallapimall.mapper.OrderMapper;
import com.ab.kkmallapimall.mapper.ProductMapper;
import com.ab.kkmallapimall.mapper.UserCouponMapper;
import com.ab.kkmallapimall.util.OrderNoGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单服务。
 *
 * 这里除了基础下单/查询能力外，还负责把订单超时字段暴露给前端，
 * 并在下单成功后触发延时关单消息。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int POINTS_PER_CURRENCY = 100;
    private static final int STOCK_UPDATE_MAX_RETRIES = 3;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final MallUserMapper mallUserMapper;
    private final CouponService couponService;
    private final PointsService pointsService;
    private final OrderTimeoutService orderTimeoutService;
    private final OrderTimeoutProperties orderTimeoutProperties;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 创建订单。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, CreateOrderRequest request) {
        Address address = addressMapper.selectById(request.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        List<Cart> carts = cartMapper.selectBatchIds(request.getCartIds());
        if (carts.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        boolean allBelongToUser = carts.stream().allMatch(cart -> cart.getUserId().equals(userId));
        if (!allBelongToUser) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        Map<Long, Product> productMap = loadProductMap(
                carts.stream().map(Cart::getProductId).collect(Collectors.toSet())
        );

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Cart cart : carts) {
            Product product = productMap.get(cart.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() == 0) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (product.getStock() < cart.getQuantity()) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
            }

            int affected = deductStockWithRetry(product.getId(), cart.getQuantity());
            ensureUpdated(affected, "商品库存已变化，请重新确认后下单");

            BigDecimal itemAmount = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
        }

        CouponCouponUsage couponUsage = resolveCouponUsage(userId, request.getUserCouponId(), totalAmount);
        PointsUsage pointsUsage = resolvePointsUsage(userId, request, totalAmount.subtract(couponUsage.discountAmount));

        BigDecimal payableAmount = totalAmount
                .subtract(couponUsage.discountAmount)
                .subtract(pointsUsage.discountAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setOrderNo(OrderNoGenerator.generate());
        order.setUserId(userId);
        order.setTotalAmount(payableAmount);
        order.setCouponId(couponUsage.couponId);
        order.setCouponAmount(couponUsage.discountAmount);
        order.setStatus(payableAmount.compareTo(BigDecimal.ZERO) == 0
                ? Constants.OrderStatus.PENDING_SHIPMENT
                : Constants.OrderStatus.PENDING_PAYMENT);
        order.setAddressId(address.getId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity()
                + address.getDistrict() + address.getDetail());
        order.setRemark(request.getRemark());
        if (payableAmount.compareTo(BigDecimal.ZERO) == 0) {
            order.setPayTime(now);
        } else {
            // 待支付订单写入明确的失效时间，便于后端关单与前端倒计时展示。
            order.setExpireTime(now.plusMinutes(orderTimeoutProperties.getTimeoutMinutes()));
        }
        orderMapper.insert(order);

        for (Cart cart : carts) {
            Product product = productMap.get(cart.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getProductName());
            orderItem.setProductImage(extractPrimaryImage(product.getImages()));
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            orderItemMapper.insert(orderItem);
        }

        if (couponUsage.userCouponId != null) {
            couponService.useCoupon(userId, couponUsage.userCouponId, order.getId());
        }
        if (pointsUsage.pointsToUse > 0) {
            pointsService.deductPoints(
                    userId,
                    pointsUsage.pointsToUse,
                    4,
                    order.getId(),
                    "Order deduction: " + order.getOrderNo()
            );
        }

        cartMapper.deleteBatchIds(request.getCartIds());

        if (order.getStatus() == Constants.OrderStatus.PENDING_PAYMENT && order.getExpireTime() != null) {
            // 只有待支付订单需要发送延时检查消息。
            applicationEventPublisher.publishEvent(
                    new OrderCreatedEvent(order.getId(), order.getOrderNo(), order.getUserId(), order.getExpireTime())
            );
        }

        return getOrderDetail(userId, order.getId());
    }

    /**
     * 查询订单列表。
     */
    public PageResult<OrderVO> getOrderList(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        Page<Order> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            if (status == Constants.OrderStatus.CANCELLED) {
                wrapper.in(Order::getStatus, Constants.OrderStatus.CANCELLED, Constants.OrderStatus.CLOSED);
            } else {
                wrapper.eq(Order::getStatus, status);
            }
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = orderMapper.selectPage(page, wrapper);
        Map<Long, List<OrderItem>> itemMap = loadOrderItemsMap(
                orderPage.getRecords().stream().map(Order::getId).toList()
        );

        List<OrderVO> voList = orderPage.getRecords().stream()
                .map(order -> convertToVO(order, itemMap.get(order.getId())))
                .collect(Collectors.toList());

        return PageResult.of(pageNum, pageSize, orderPage.getTotal(), voList);
    }

    /**
     * 查询订单详情。
     */
    public OrderVO getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return convertToVO(order, loadOrderItems(orderId));
    }

    /**
     * 用户主动取消订单。
     * 具体回补逻辑统一收敛到 OrderTimeoutService，避免多处重复实现。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Constants.OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        boolean cancelled = orderTimeoutService.cancelPendingPaymentOrder(orderId);
        if (!cancelled) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
    }

    /**
     * 确认收货。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (order.getStatus() != Constants.OrderStatus.PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CONFIRM);
        }

        order.setStatus(Constants.OrderStatus.COMPLETED);
        order.setConfirmTime(LocalDateTime.now());
        int confirmAffected = orderMapper.updateById(order);
        ensureUpdated(confirmAffected, "订单状态已变化，请刷新后重试");
    }

    /**
     * 删除订单（逻辑删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (order.getStatus() != Constants.OrderStatus.COMPLETED
                && order.getStatus() != Constants.OrderStatus.CANCELLED
                && order.getStatus() != Constants.OrderStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getDeleted, Instant.now().toEpochMilli())
                .eq(Order::getId, orderId)
                .isNull(Order::getDeleted));
    }

    /**
     * 后台发货。
     */
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String logisticsCompany, String trackingNumber) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (order.getStatus() != Constants.OrderStatus.PENDING_SHIPMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        order.setStatus(Constants.OrderStatus.PENDING_RECEIPT);
        order.setLogisticsCompany(logisticsCompany);
        order.setTrackingNumber(trackingNumber);
        order.setShipTime(LocalDateTime.now());
        int shipAffected = orderMapper.updateById(order);
        ensureUpdated(shipAffected, "订单状态已变化，请刷新后重试");
    }

    private OrderVO convertToVO(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        // 前端直接使用剩余秒数做倒计时，避免自己重复计算业务规则。
        vo.setRemainingSeconds(calculateRemainingSeconds(order));
        List<OrderItemVO> itemVOList = (items == null ? Collections.<OrderItem>emptyList() : items)
                .stream()
                .map(item -> {
                    OrderItemVO itemVO = new OrderItemVO();
                    BeanUtils.copyProperties(item, itemVO);
                    return itemVO;
                })
                .collect(Collectors.toList());
        vo.setItems(itemVOList);
        return vo;
    }

    private Long calculateRemainingSeconds(Order order) {
        if (order == null
                || order.getStatus() == null
                || order.getStatus() != Constants.OrderStatus.PENDING_PAYMENT
                || order.getExpireTime() == null) {
            return 0L;
        }
        long remaining = Duration.between(LocalDateTime.now(), order.getExpireTime()).getSeconds();
        return Math.max(remaining, 0L);
    }

    private List<OrderItem> loadOrderItems(Long orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
        );
    }

    private Map<Long, List<OrderItem>> loadOrderItemsMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
        ).stream().collect(Collectors.groupingBy(
                OrderItem::getOrderId,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    private Map<Long, Product> loadProductMap(Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    private CouponCouponUsage resolveCouponUsage(Long userId, Long userCouponId, BigDecimal totalAmount) {
        if (userCouponId == null) {
            return new CouponCouponUsage(null, null, BigDecimal.ZERO);
        }

        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }
        if (userCoupon.getStatus() != 0 || LocalDateTime.now().isAfter(userCoupon.getExpireTime())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (coupon.getMinAmount() != null && totalAmount.compareTo(coupon.getMinAmount()) < 0) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        BigDecimal discountAmount;
        if (coupon.getType() != null && coupon.getType() == 1) {
            discountAmount = coupon.getDiscountAmount() == null ? BigDecimal.ZERO : coupon.getDiscountAmount();
        } else {
            BigDecimal rate = coupon.getDiscountRate() == null ? BigDecimal.TEN : coupon.getDiscountRate();
            discountAmount = totalAmount.subtract(
                    totalAmount.multiply(rate).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP)
            );
        }

        if (discountAmount.compareTo(totalAmount) > 0) {
            discountAmount = totalAmount;
        }

        return new CouponCouponUsage(userCouponId, coupon.getId(), discountAmount.max(BigDecimal.ZERO));
    }

    private PointsUsage resolvePointsUsage(Long userId, CreateOrderRequest request, BigDecimal payableBeforePoints) {
        if (request.getUsePoints() == null || !request.getUsePoints() || payableBeforePoints.compareTo(BigDecimal.ZERO) <= 0) {
            return new PointsUsage(0, BigDecimal.ZERO);
        }

        MallUser user = mallUserMapper.selectById(userId);
        int currentPoints = user == null || user.getPoints() == null ? 0 : user.getPoints();
        int requestedPoints = request.getPointsToUse() == null || request.getPointsToUse() <= 0
                ? currentPoints
                : request.getPointsToUse();
        int maxPointsByAmount = payableBeforePoints.multiply(BigDecimal.valueOf(POINTS_PER_CURRENCY))
                .setScale(0, RoundingMode.DOWN)
                .intValue();
        int pointsToUse = Math.min(currentPoints, Math.min(requestedPoints, maxPointsByAmount));
        BigDecimal discountAmount = BigDecimal.valueOf(pointsToUse)
                .divide(BigDecimal.valueOf(POINTS_PER_CURRENCY), 2, RoundingMode.DOWN);

        return new PointsUsage(pointsToUse, discountAmount);
    }

    private String extractPrimaryImage(String images) {
        if (!StringUtils.hasText(images)) {
            return null;
        }
        String[] values = images.split(",");
        return values.length > 0 ? values[0] : null;
    }

    private int deductStockWithRetry(Long productId, Integer quantity) {
        // 下单扣库存采用简单重试，配合乐观锁减少并发写冲突。
        int required = quantity == null ? 0 : quantity;
        if (required <= 0) {
            return 1;
        }
        for (int attempt = 1; attempt <= STOCK_UPDATE_MAX_RETRIES; attempt++) {
            Product latest = productMapper.selectById(productId);
            if (latest == null || latest.getStatus() == null || latest.getStatus() == 0) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            int currentStock = latest.getStock() == null ? 0 : latest.getStock();
            if (currentStock < required) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
            }
            latest.setStock(currentStock - required);
            if (productMapper.updateById(latest) > 0) {
                return 1;
            }
        }
        return 0;
    }

    private void ensureUpdated(int affected, String message) {
        if (affected > 0) {
            return;
        }
        throw new BusinessException(409, message);
    }

    private record CouponCouponUsage(Long userCouponId, Long couponId, BigDecimal discountAmount) {
    }

    private record PointsUsage(int pointsToUse, BigDecimal discountAmount) {
    }
}
