package com.ab.kkmallapimall.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求
 */
@Data
public class CreateOrderRequest {

    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;

    @NotEmpty(message = "购物车ID列表不能为空")
    private List<Long> cartIds;

    private String remark;
}
