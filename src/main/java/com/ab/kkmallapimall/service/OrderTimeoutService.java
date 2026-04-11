package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.entity.Order;
import com.ab.kkmallapimall.entity.OrderItem;
import com.ab.kkmallapimall.entity.Payment;
import com.ab.kkmallapimall.entity.Product;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.mapper.OrderItemMapper;
import com.ab.kkmallapimall.mapper.OrderMapper;
import com.ab.kkmallapimall.mapper.PaymentMapper;
import com.ab.kkmallapimall.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 订单超时关闭核心服务。
 *
 * 职责：
 * 1. 关闭待支付订单；
 * 2. 回补库存；
 * 3. 回退优惠券和积分；
 * 4. 将待支付的支付单标记为失败。
 */
public class OrderTimeoutService {

    private static final int STOCK_UPDATE_MAX_RETRIES = 3;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final PaymentMapper paymentMapper;
    private final CouponService couponService;
    private final PointsService pointsService;

    /**
     * 判断订单是否已经进入“待支付超时”状态。
     */
    public boolean isPendingPaymentExpired(Order order) {
        return order != null
                && order.getStatus() != null
                && order.getStatus() == Constants.OrderStatus.PENDING_PAYMENT
                && order.getExpireTime() != null
                && !order.getExpireTime().isAfter(LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelPendingPaymentOrder(Long orderId) {
        return closePendingPaymentOrder(orderId, Constants.OrderStatus.CANCELLED, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean closeExpiredOrder(Long orderId) {
        return closePendingPaymentOrder(orderId, Constants.OrderStatus.CLOSED, true);
    }

    /**
     * 兜底扫描超时订单，避免消息丢失后无人关单。
     */
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders(int batchSize) {
        List<Order> expiredOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, Constants.OrderStatus.PENDING_PAYMENT)
                        .isNotNull(Order::getExpireTime)
                        .le(Order::getExpireTime, LocalDateTime.now())
                        .orderByAsc(Order::getExpireTime)
                        .last("LIMIT " + Math.max(batchSize, 1))
        );
        int closedCount = 0;
        for (Order order : expiredOrders) {
            if (closePendingPaymentOrder(order.getId(), Constants.OrderStatus.CLOSED, true)) {
                closedCount++;
            }
        }
        return closedCount;
    }

    private boolean closePendingPaymentOrder(Long orderId, int targetStatus, boolean requireExpired) {
        // 先通过订单状态做一次幂等保护，避免重复回补库存。
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() == null || order.getStatus() != Constants.OrderStatus.PENDING_PAYMENT) {
            return false;
        }
        if (requireExpired) {
            LocalDateTime expireTime = order.getExpireTime();
            if (expireTime == null || expireTime.isAfter(LocalDateTime.now())) {
                return false;
            }
        }

        order.setStatus(targetStatus);
        int orderAffected = orderMapper.updateById(order);
        if (orderAffected <= 0) {
            return false;
        }

        restoreStock(order.getId());
        failPendingPayments(order.getId());
        couponService.releaseCouponForOrder(order.getUserId(), order.getId());
        pointsService.restorePointsForOrder(
                order.getUserId(),
                order.getId(),
                targetStatus == Constants.OrderStatus.CLOSED
                        ? "Order timeout refund: " + order.getOrderNo()
                        : "Order cancellation refund: " + order.getOrderNo()
        );
        log.info("Closed pending payment order. orderId={}, orderNo={}, targetStatus={}",
                order.getId(), order.getOrderNo(), targetStatus);
        return true;
    }

    /**
     * 回补订单商品库存。
     */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        for (OrderItem item : items) {
            int affected = increaseStockWithRetry(item.getProductId(), item.getQuantity());
            ensureUpdated(affected, "商品库存已变化，请刷新后重试");
        }
    }

    /**
     * 将待支付中的支付单统一置为失败，避免过期订单继续支付。
     */
    private void failPendingPayments(Long orderId) {
        List<Payment> pendingPayments = paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getStatus, 0)
        );
        for (Payment payment : pendingPayments) {
            payment.setStatus(2);
            int affected = paymentMapper.updateById(payment);
            ensureUpdated(affected, "支付状态已变化，请刷新后重试");
        }
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
}
