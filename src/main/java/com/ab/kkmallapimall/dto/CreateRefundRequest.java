package com.ab.kkmallapimall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建退款请求
 */
@Data
public class CreateRefundRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "退款金额不能为空")
    private BigDecimal refundAmount;

    @NotNull(message = "退款类型不能为空")
    private Integer refundType; // 1-仅退款，2-退货退款

    @NotBlank(message = "退款原因不能为空")
    private String reason;

    private String description;

    private String images;
}
