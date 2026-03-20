package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.CouponVO;
import com.ab.kkmallapimall.dto.UserCouponVO;
import com.ab.kkmallapimall.service.CouponService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 优惠券控制器
 */
@Tag(name = "优惠券管理", description = "优惠券相关接口")
@RestController
@RequestMapping("/api/mall/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "获取可领取优惠券列表")
    @GetMapping("/available")
    public Result<PageResult<CouponVO>> getAvailableCoupons(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<CouponVO> result = couponService.getAvailableCoupons(pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/{couponId}/receive")
    public Result<Void> receiveCoupon(@PathVariable Long couponId) {
        Long userId = securityUtils.getCurrentUserId();
        couponService.receiveCoupon(userId, couponId);
        return Result.success("领取成功", null);
    }

    @Operation(summary = "获取我的优惠券")
    @GetMapping("/my")
    public Result<PageResult<UserCouponVO>> getMyCoupons(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtils.getCurrentUserId();
        PageResult<UserCouponVO> result = couponService.getUserCoupons(userId, status, pageNum, pageSize);
        return Result.success(result);
    }
}
