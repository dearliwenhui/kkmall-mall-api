package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.CreateRefundRequest;
import com.ab.kkmallapimall.dto.RefundVO;
import com.ab.kkmallapimall.service.RefundService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 退款控制器
 */
@Tag(name = "退款管理", description = "退款相关接口")
@RestController
@RequestMapping("/api/mall/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "创建退款申请")
    @PostMapping
    public Result<RefundVO> createRefund(@Valid @RequestBody CreateRefundRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        RefundVO refund = refundService.createRefund(userId, request);
        return Result.success(refund);
    }

    @Operation(summary = "获取退款列表")
    @GetMapping
    public Result<PageResult<RefundVO>> getRefundList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtils.getCurrentUserId();
        PageResult<RefundVO> result = refundService.getRefundList(userId, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "获取退款详情")
    @GetMapping("/{id}")
    public Result<RefundVO> getRefundDetail(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        RefundVO refund = refundService.getRefundDetail(userId, id);
        return Result.success(refund);
    }
}
