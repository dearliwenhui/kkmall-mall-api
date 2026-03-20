package com.ab.kkmallapimall.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录视图对象
 */
@Data
public class PaymentVO {

    /**
     * 支付ID
     */
    private Long id;

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式（1-支付宝，2-微信，3-模拟支付）
     */
    private Integer paymentType;

    /**
     * 支付方式名称
     */
    private String paymentTypeName;

    /**
     * 支付状态（0-待支付，1-支付成功，2-支付失败）
     */
    private Integer status;

    /**
     * 支付状态名称
     */
    private String statusName;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 第三方支付流水号
     */
    private String thirdPartyNo;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
