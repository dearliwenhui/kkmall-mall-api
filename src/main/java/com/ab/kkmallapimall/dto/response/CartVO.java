package com.ab.kkmallapimall.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车VO
 */
@Data
public class CartVO {

    private Long id;

    private Long productId;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private Integer quantity;

    private Integer selected;

    private Integer stock;

    /**
     * 小计金额
     */
    private BigDecimal subtotal;
}
