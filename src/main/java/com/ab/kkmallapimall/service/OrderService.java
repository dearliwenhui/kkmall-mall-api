package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.dto.request.CreateOrderRequest;
import com.ab.kkmallapimall.dto.response.OrderItemVO;
import com.ab.kkmallapimall.dto.response.OrderVO;
import com.ab.kkmallapimall.entity.*;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.*;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;

    /**
     * 创建订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, CreateOrderRequest request) {
        // 查询收货地址
        Address address = addressMapper.selectById(request.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        // 查询购物车商品
        List<Cart> carts = cartMapper.selectBatchIds(request.getCartIds());
        if (carts.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        // 验证购物车商品属于当前用户
        boolean allBelongToUser = carts.stream().allMatch(cart -> cart.getUserId().equals(userId));
        if (!allBelongToUser) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 计算订单总金额并扣减库存
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Cart cart : carts) {
            Product product = productMapper.selectById(cart.getProductId());
            if (product == null || product.getStatus() == 0) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            // 检查库存并扣减
            if (product.getStock() < cart.getQuantity()) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
            }
            product.setStock(product.getStock() - cart.getQuantity());
            productMapper.updateById(product);

            // 计算金额
            BigDecimal itemAmount = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo(OrderNoGenerator.generate());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(Constants.OrderStatus.PENDING_PAYMENT);
        order.setAddressId(address.getId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() +
                address.getDistrict() + address.getDetail());
        order.setRemark(request.getRemark());
        orderMapper.insert(order);

        // 创建订单明细
        for (Cart cart : carts) {
            Product product = productMapper.selectById(cart.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getProductName());

            // 获取主图
            if (StringUtils.hasText(product.getImages())) {
                String[] images = product.getImages().split(",");
                orderItem.setProductImage(images.length > 0 ? images[0] : null);
            }

            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            orderItemMapper.insert(orderItem);
        }

        // 清空购物车
        cartMapper.deleteBatchIds(request.getCartIds());

        return getOrderDetail(userId, order.getId());
    }

    /**
     * 获取订单列表
     */
    public PageResult<OrderVO> getOrderList(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        Page<Order> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = orderMapper.selectPage(page, wrapper);

        List<OrderVO> voList = orderPage.getRecords().stream()
                .map(order -> convertToVO(order, true))
                .collect(Collectors.toList());

        return PageResult.of(pageNum, pageSize, orderPage.getTotal(), voList);
    }

    /**
     * 获取订单详情
     */
    public OrderVO getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return convertToVO(order, true);
    }

    /**
     * 取消订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 只有待付款状态可以取消
        if (order.getStatus() != Constants.OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
        );

        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
            }
        }

        // 更新订单状态
        order.setStatus(Constants.OrderStatus.CANCELLED);
        orderMapper.updateById(order);
    }

    /**
     * 确认收货
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 只有待收货状态可以确认收货
        if (order.getStatus() != Constants.OrderStatus.PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CONFIRM);
        }

        // 更新订单状态
        order.setStatus(Constants.OrderStatus.COMPLETED);
        order.setConfirmTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 删除订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 只有已完成或已取消的订单可以删除
        if (order.getStatus() != Constants.OrderStatus.COMPLETED &&
                order.getStatus() != Constants.OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getDeleted, Instant.now().toEpochMilli())
                .eq(Order::getId, orderId)
                .isNull(Order::getDeleted));
    }

    /**
     * 发货（管理后台使用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String logisticsCompany, String trackingNumber) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 只有待发货状态可以发货
        if (order.getStatus() != Constants.OrderStatus.PENDING_SHIPMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 更新订单状态和物流信息
        order.setStatus(Constants.OrderStatus.PENDING_RECEIPT);
        order.setLogisticsCompany(logisticsCompany);
        order.setTrackingNumber(trackingNumber);
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 转换为VO
     */
    private OrderVO convertToVO(Order order, boolean includeItems) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        if (includeItems) {
            // 查询订单明细
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, order.getId())
            );

            List<OrderItemVO> itemVOList = items.stream()
                    .map(item -> {
                        OrderItemVO itemVO = new OrderItemVO();
                        BeanUtils.copyProperties(item, itemVO);
                        return itemVO;
                    })
                    .collect(Collectors.toList());

            vo.setItems(itemVOList);
        }

        return vo;
    }
}
