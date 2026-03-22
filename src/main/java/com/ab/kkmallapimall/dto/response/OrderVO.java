package com.ab.kkmallapimall.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单VO
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private BigDecimal totalAmount;

    private BigDecimal couponAmount;

    private Integer status;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime confirmTime;

    private LocalDateTime createTime;

    /**
     * 订单明细列表
     */
    private List<OrderItemVO> items;
}
