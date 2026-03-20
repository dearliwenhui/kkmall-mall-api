package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.LogisticsVO;
import com.ab.kkmallapimall.service.LogisticsService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 物流控制器
 */
@Tag(name = "物流跟踪", description = "物流跟踪相关接口")
@RestController
@RequestMapping("/api/mall/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "查询物流信息")
    @GetMapping("/order/{orderId}")
    public Result<LogisticsVO> getLogistics(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        LogisticsVO logistics = logisticsService.getLogistics(userId, orderId);
        return Result.success(logistics);
    }
}
