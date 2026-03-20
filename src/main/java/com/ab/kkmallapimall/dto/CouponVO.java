package com.ab.kkmallapimall.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券视图对象
 */
@Data
public class CouponVO {

    private Long id;

    private String name;

    private Integer type;

    private String typeName;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal minAmount;

    private Integer totalCount;

    private Integer receivedCount;

    private Integer validDays;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private String statusName;

    private LocalDateTime createTime;
}
