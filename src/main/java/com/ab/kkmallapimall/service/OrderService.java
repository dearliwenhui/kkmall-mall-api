package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.common.PageResult;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Order service.
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

    /**
     * Create order.
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
            if (product == null || product.getStatus() == 0) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            if (product.getStock() < cart.getQuantity()) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
            }
            int affected = deductStockWithRetry(product.getId(), cart.getQuantity());
            ensureUpdated(affected, "商品库存已变更，请重新确认后下单");

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
            order.setPayTime(LocalDateTime.now());
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

        return getOrderDetail(userId, order.getId());
    }

    /**
     * Get order list.
     */
    public PageResult<OrderVO> getOrderList(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        Page<Order> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime);

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
     * Get order detail.
     */
    public OrderVO getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return convertToVO(order, loadOrderItems(orderId));
    }

    /**
     * Cancel order.
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

        List<OrderItem> items = loadOrderItems(orderId);
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                int affected = increaseStockWithRetry(product.getId(), item.getQuantity());
                ensureUpdated(affected, "商品库存已变更，请刷新后重试");
            }
        }

        order.setStatus(Constants.OrderStatus.CANCELLED);
        int cancelAffected = orderMapper.updateById(order);
        ensureUpdated(cancelAffected, "订单状态已变化，请刷新后重试");
    }

    /**
     * Confirm receipt.
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
     * Delete order.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (order.getStatus() != Constants.OrderStatus.COMPLETED
                && order.getStatus() != Constants.OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getDeleted, Instant.now().toEpochMilli())
                .eq(Order::getId, orderId)
                .isNull(Order::getDeleted));
    }

    /**
     * Ship order for admin.
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

    private int increaseStockWithRetry(Long productId, Integer quantity) {
        int delta = quantity == null ? 0 : quantity;
        if (delta <= 0) {
            return 1;
        }
        for (int attempt = 1; attempt <= STOCK_UPDATE_MAX_RETRIES; attempt++) {
            Product latest = productMapper.selectById(productId);
            if (latest == null) {
                return 1;
            }
            int currentStock = latest.getStock() == null ? 0 : latest.getStock();
            latest.setStock(currentStock + delta);
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
