package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.dto.CreateReviewRequest;
import com.ab.kkmallapimall.dto.ReviewVO;
import com.ab.kkmallapimall.entity.Order;
import com.ab.kkmallapimall.entity.Review;
import com.ab.kkmallapimall.entity.MallUser;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.OrderMapper;
import com.ab.kkmallapimall.mapper.ReviewMapper;
import com.ab.kkmallapimall.mapper.MallUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价服务
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final MallUserMapper mallUserMapper;

    /**
     * 创建评价
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO createReview(Long userId, CreateReviewRequest request) {
        // 验证订单
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 验证订单状态（只有已完成的订单可以评价）
        if (order.getStatus() != 4) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 检查是否已评价
        Review existingReview = reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderItemId, request.getOrderItemId())
                        .eq(Review::getUserId, userId)
        );
        if (existingReview != null) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 创建评价
        Review review = new Review();
        review.setOrderId(request.getOrderId());
        review.setOrderItemId(request.getOrderItemId());
        review.setProductId(request.getProductId());
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setImages(request.getImages());
        review.setStatus(1); // 显示
        reviewMapper.insert(review);

        return convertToVO(review);
    }

    /**
     * 获取商品评价列表
     */
    public PageResult<ReviewVO> getProductReviews(Long productId, Integer pageNum, Integer pageSize) {
        Page<Review> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getProductId, productId)
                .eq(Review::getStatus, 1)
                .orderByDesc(Review::getCreateTime);

        Page<Review> reviewPage = reviewMapper.selectPage(page, wrapper);

        List<ReviewVO> voList = reviewPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(pageNum, pageSize, reviewPage.getTotal(), voList);
    }

    /**
     * 获取用户评价列表
     */
    public PageResult<ReviewVO> getUserReviews(Long userId, Integer pageNum, Integer pageSize) {
        Page<Review> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, userId)
                .orderByDesc(Review::getCreateTime);

        Page<Review> reviewPage = reviewMapper.selectPage(page, wrapper);

        List<ReviewVO> voList = reviewPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(pageNum, pageSize, reviewPage.getTotal(), voList);
    }

    /**
     * 转换为VO
     */
    private ReviewVO convertToVO(Review review) {
        ReviewVO vo = new ReviewVO();
        BeanUtils.copyProperties(review, vo);

        // 查询用户信息
        MallUser user = mallUserMapper.selectById(review.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setUserAvatar(user.getAvatar());
        }

        return vo;
    }
}
