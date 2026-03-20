package com.ab.kkmallapimall.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 选中商品请求
 */
@Data
public class SelectItemsRequest {

    @NotEmpty(message = "购物车ID列表不能为空")
    private List<Long> ids;

    private Boolean selected;
}
