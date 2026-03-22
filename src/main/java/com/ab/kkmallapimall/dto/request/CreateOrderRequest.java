package com.ab.kkmallapimall.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Create order request.
 */
@Data
public class CreateOrderRequest {

    @NotNull(message = "addressId is required")
    private Long addressId;

    @NotEmpty(message = "cartIds cannot be empty")
    private List<Long> cartIds;

    private Long userCouponId;

    private Boolean usePoints;

    private Integer pointsToUse;

    private String remark;
}
