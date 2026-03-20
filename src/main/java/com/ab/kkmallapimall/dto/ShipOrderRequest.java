package com.ab.kkmallapimall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发货请求 DTO
 */
@Data
public class ShipOrderRequest {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 物流公司
     */
    @NotBlank(message = "物流公司不能为空")
    private String logisticsCompany;

    /**
     * 物流单号
     */
    @NotBlank(message = "物流单号不能为空")
    private String trackingNumber;
}
