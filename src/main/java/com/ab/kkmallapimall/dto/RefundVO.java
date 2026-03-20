package com.ab.kkmallapimall.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款视图对象
 */
@Data
public class RefundVO {

    private Long id;

    private String refundNo;

    private Long orderId;

    private String orderNo;

    private BigDecimal refundAmount;

    private Integer refundType;

    private String refundTypeName;

    private String reason;

    private String description;

    private String images;

    private Integer status;

    private String statusName;

    private String rejectReason;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;
}
