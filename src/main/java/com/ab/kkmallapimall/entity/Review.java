package com.ab.kkmallapimall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品评价实体类
 */
@Data
@TableName("mall_review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long orderItemId;

    private Long productId;

    private Long userId;

    private Integer rating;

    private String content;

    private String images;

    private String reply;

    private LocalDateTime replyTime;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
