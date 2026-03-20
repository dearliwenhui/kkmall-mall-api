package com.ab.kkmallapimall.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品VO
 */
@Data
public class ProductVO {

    private Long id;

    private String productName;

    private String productCode;

    private Long categoryId;

    private String categoryName;

    private BigDecimal price;

    private Integer stock;

    private String description;

    private Integer status;

    /**
     * 图片列表
     */
    private List<String> imageList;

    /**
     * 主图
     */
    private String mainImage;
}
