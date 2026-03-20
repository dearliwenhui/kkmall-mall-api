package com.ab.kkmallapimall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（只读，由管理后台维护）
 */
@Data
@TableName("mall_product")
public class Product implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String productName;

    private String productCode;

    private Long categoryId;

    private BigDecimal price;

    private Integer stock;

    private String description;

    /**
     * 1: on sale, 0: off sale
     */
    private Integer status;

    /**
     * Comma-separated image URLs
     */
    private String images;

    @TableLogic(value = "null", delval = "1")
    private Long deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
