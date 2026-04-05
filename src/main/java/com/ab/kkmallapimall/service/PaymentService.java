package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.dto.CreatePaymentRequest;
import com.ab.kkmallapimall.dto.PaymentVO;
import com.ab.kkmallapimall.entity.Order;
import com.ab.kkmallapimall.entity.Payment;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.OrderMapper;
import com.ab.kkmallapimall.mapper.PaymentMapper;
import com.ab.kkmallapimall.util.PaymentNoGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付服务
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    /**
     * 创建支付
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO createPayment(Long userId, CreatePaymentRequest request) {
        // 查询订单
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 验证订单状态
        if (order.getStatus() != Constants.OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 检查是否已有支付记录
        Payment existingPayment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, order.getId())
                        .eq(Payment::getStatus, 0)
        );
        if (existingPayment != null) {
            return convertToVO(existingPayment);
        }

        // 创建支付记录
        Payment payment = new Payment();
        payment.setPaymentNo(PaymentNoGenerator.generate());
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(userId);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentType(request.getPaymentType());
        payment.setStatus(0); // 待支付
        paymentMapper.insert(payment);

        return convertToVO(payment);
    }

    /**
     * 处理支付（模拟支付）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> processPayment(Long userId, String paymentNo) {
        // 查询支付记录
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPaymentNo, paymentNo)
        );

        if (payment == null || !payment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        // 验证支付状态
        if (payment.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_ERROR);
        }

        // 模拟支付成功
        payment.setStatus(1); // 支付成功
        payment.setPayTime(LocalDateTime.now());
        payment.setThirdPartyNo("MOCK" + System.currentTimeMillis());
        int paymentAffected = paymentMapper.updateById(payment);
        ensureUpdated(paymentAffected, "数据已变化，请刷新后重试");

        // 更新订单状态
        Order order = orderMapper.selectById(payment.getOrderId());
        if (order != null) {
            order.setStatus(Constants.OrderStatus.PENDING_SHIPMENT);
            order.setPayTime(LocalDateTime.now());
            int orderAffected = orderMapper.updateById(order);
            ensureUpdated(orderAffected, "订单状态已变化，请刷新后重试");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "支付成功");
        result.put("paymentNo", paymentNo);
        result.put("orderId", payment.getOrderId());
        return result;
    }

    /**
     * 查询支付记录
     */
    public PaymentVO getPaymentByOrderId(Long userId, Long orderId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .orderByDesc(Payment::getCreateTime)
                        .last("LIMIT 1")
        );

        if (payment == null || !payment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        return convertToVO(payment);
    }

    /**
     * 根据支付流水号查询
     */
    public PaymentVO getPaymentByNo(Long userId, String paymentNo) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPaymentNo, paymentNo)
        );

        if (payment == null || !payment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        return convertToVO(payment);
    }

    /**
     * 转换为VO
     */
    private PaymentVO convertToVO(Payment payment) {
        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(payment, vo);

        // 设置支付方式名称
        vo.setPaymentTypeName(getPaymentTypeName(payment.getPaymentType()));

        // 设置支付状态名称
        vo.setStatusName(getPaymentStatusName(payment.getStatus()));

        return vo;
    }

    /**
     * 获取支付方式名称
     */
    private String getPaymentTypeName(Integer paymentType) {
        return switch (paymentType) {
            case 1 -> "支付宝";
            case 2 -> "微信支付";
            case 3 -> "模拟支付";
            default -> "未知";
        };
    }

    /**
     * 获取支付状态名称
     */
    private String getPaymentStatusName(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "支付成功";
            case 2 -> "支付失败";
            default -> "未知";
        };
    }

    private void ensureUpdated(int affected, String message) {
        if (affected > 0) {
            return;
        }
        throw new BusinessException(409, message);
    }
}
