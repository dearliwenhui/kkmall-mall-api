package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.dto.CouponVO;
import com.ab.kkmallapimall.dto.UserCouponVO;
import com.ab.kkmallapimall.entity.Coupon;
import com.ab.kkmallapimall.entity.UserCoupon;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.CouponMapper;
import com.ab.kkmallapimall.mapper.UserCouponMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券服务
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    /**
     * 获取可领取优惠券列表
     */
    public PageResult<CouponVO> getAvailableCoupons(Integer pageNum, Integer pageSize) {
        Page<Coupon> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, 1)
                .le(Coupon::getStartTime, LocalDateTime.now())
                .ge(Coupon::getEndTime, LocalDateTime.now())
                .orderByDesc(Coupon::getCreateTime);

        Page<Coupon> couponPage = couponMapper.selectPage(page, wrapper);

        List<CouponVO> voList = couponPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(pageNum, pageSize, couponPage.getTotal(), voList);
    }

    /**
     * 领取优惠券
     */
    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(Long userId, Long couponId) {
        // 查询优惠券
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }

        // 验证优惠券状态
        if (coupon.getStatus() != 1) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        // 验证时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        // 验证库存
        if (coupon.getReceivedCount() >= coupon.getTotalCount()) {
            throw new BusinessException(ErrorCode.COUPON_STOCK_NOT_ENOUGH);
        }

        // 检查是否已领取
        Long count = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId)
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_RECEIVED);
        }

        // 创建用户优惠券
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0); // 未使用
        userCoupon.setExpireTime(now.plusDays(coupon.getValidDays()));
        userCouponMapper.insert(userCoupon);

        // 更新优惠券领取数量
        coupon.setReceivedCount(coupon.getReceivedCount() + 1);
        int affected = couponMapper.updateById(coupon);
        ensureUpdated(affected, "数据已变化，请刷新后重试");
    }

    /**
     * 获取用户优惠券列表
     */
    public PageResult<UserCouponVO> getUserCoupons(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        Page<UserCoupon> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status)
                .orderByDesc(UserCoupon::getCreateTime);

        Page<UserCoupon> userCouponPage = userCouponMapper.selectPage(page, wrapper);

        List<UserCouponVO> voList = userCouponPage.getRecords().stream()
                .map(this::convertToUserCouponVO)
                .collect(Collectors.toList());

        return PageResult.of(pageNum, pageSize, userCouponPage.getTotal(), voList);
    }

    /**
     * 使用优惠券
     */
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(Long userId, Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }

        if (userCoupon.getStatus() != 0) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        if (LocalDateTime.now().isAfter(userCoupon.getExpireTime())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        userCoupon.setStatus(1); // 已使用
        userCoupon.setUsedTime(LocalDateTime.now());
        userCoupon.setOrderId(orderId);
        int affected = userCouponMapper.updateById(userCoupon);
        ensureUpdated(affected, "数据已变化，请刷新后重试");
    }

    /**
     * 转换为VO
     */
    private CouponVO convertToVO(Coupon coupon) {
        CouponVO vo = new CouponVO();
        BeanUtils.copyProperties(coupon, vo);
        vo.setTypeName(coupon.getType() == 1 ? "满减券" : "折扣券");
        vo.setStatusName(coupon.getStatus() == 1 ? "可用" : "不可用");
        return vo;
    }

    /**
     * 转换为用户优惠券VO
     */
    private UserCouponVO convertToUserCouponVO(UserCoupon userCoupon) {
        UserCouponVO vo = new UserCouponVO();
        BeanUtils.copyProperties(userCoupon, vo);

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon != null) {
            vo.setCouponName(coupon.getName());
            vo.setType(coupon.getType());
            vo.setTypeName(coupon.getType() == 1 ? "满减券" : "折扣券");
            vo.setDiscountAmount(coupon.getDiscountAmount());
            vo.setDiscountRate(coupon.getDiscountRate());
            vo.setMinAmount(coupon.getMinAmount());
        }

        vo.setStatusName(switch (userCoupon.getStatus()) {
            case 0 -> "未使用";
            case 1 -> "已使用";
            case 2 -> "已过期";
            default -> "未知";
        });

        return vo;
    }

    private void ensureUpdated(int affected, String message) {
        if (affected > 0) {
            return;
        }
        throw new BusinessException(409, message);
    }
}
