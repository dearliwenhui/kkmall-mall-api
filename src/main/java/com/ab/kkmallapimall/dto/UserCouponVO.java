package com.ab.kkmallapimall.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券视图对象
 */
@Data
public class UserCouponVO {

    private Long id;

    private Long couponId;

    private String couponName;

    private Integer type;

    private String typeName;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal minAmount;

    private Integer status;

    private String statusName;

    private LocalDateTime usedTime;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}
