package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.CreateReviewRequest;
import com.ab.kkmallapimall.dto.ReviewVO;
import com.ab.kkmallapimall.service.ReviewService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评价控制器
 */
@Tag(name = "商品评价", description = "商品评价相关接口")
@RestController
@RequestMapping("/api/mall/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "创建评价")
    @PostMapping
    public Result<ReviewVO> createReview(@Valid @RequestBody CreateReviewRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        ReviewVO review = reviewService.createReview(userId, request);
        return Result.success(review);
    }

    @Operation(summary = "获取商品评价列表")
    @GetMapping("/product/{productId}")
    public Result<PageResult<ReviewVO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<ReviewVO> result = reviewService.getProductReviews(productId, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "获取我的评价列表")
    @GetMapping("/my")
    public Result<PageResult<ReviewVO>> getMyReviews(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtils.getCurrentUserId();
        PageResult<ReviewVO> result = reviewService.getUserReviews(userId, pageNum, pageSize);
        return Result.success(result);
    }
}
