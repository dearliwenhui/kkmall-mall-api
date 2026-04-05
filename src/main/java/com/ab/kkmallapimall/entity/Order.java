package com.ab.kkmallapimall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("mall_order")
public class Order implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private Long couponId;

    private BigDecimal couponAmount;

    private Integer status;

    private Long addressId;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String logisticsCompany;

    private String trackingNumber;

    private String remark;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime confirmTime;

    @Version
    private Long version;

    @TableLogic(value = "NULL", delval = "UNIX_TIMESTAMP(NOW()) * 1000")
    private Long deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
