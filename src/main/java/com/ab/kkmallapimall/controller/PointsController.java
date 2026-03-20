package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.service.PointsService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 积分控制器
 */
@Tag(name = "积分管理", description = "积分相关接口")
@RestController
@RequestMapping("/api/mall/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "获取用户积分")
    @GetMapping
    public Result<Integer> getUserPoints() {
        Long userId = securityUtils.getCurrentUserId();
        Integer points = pointsService.getUserPoints(userId);
        return Result.success(points);
    }

    @Operation(summary = "获取积分记录")
    @GetMapping("/log")
    public Result<PageResult<Map<String, Object>>> getPointsLog(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtils.getCurrentUserId();
        PageResult<Map<String, Object>> result = pointsService.getPointsLog(userId, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "签到")
    @PostMapping("/sign-in")
    public Result<Void> signIn() {
        Long userId = securityUtils.getCurrentUserId();
        pointsService.signIn(userId);
        return Result.success("签到成功，获得5积分", null);
    }
}
