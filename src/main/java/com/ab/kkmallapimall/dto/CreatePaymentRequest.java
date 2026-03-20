package com.ab.kkmallapimall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建支付请求 DTO
 */
@Data
public class CreatePaymentRequest {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 支付方式（1-支付宝，2-微信，3-模拟支付）
     */
    @NotNull(message = "支付方式不能为空")
    private Integer paymentType;
}
