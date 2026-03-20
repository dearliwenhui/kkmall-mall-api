package com.ab.kkmallapimall.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价视图对象
 */
@Data
public class ReviewVO {

    private Long id;

    private Long orderId;

    private Long orderItemId;

    private Long productId;

    private Long userId;

    private String username;

    private String userAvatar;

    private Integer rating;

    private String content;

    private String images;

    private String reply;

    private LocalDateTime replyTime;

    private LocalDateTime createTime;
}
