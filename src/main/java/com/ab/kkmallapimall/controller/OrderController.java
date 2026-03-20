package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.request.CreateOrderRequest;
import com.ab.kkmallapimall.dto.response.OrderVO;
import com.ab.kkmallapimall.service.OrderService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Tag(name = "订单管理", description = "订单相关接口")
@RestController
@RequestMapping("/api/mall/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<OrderVO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        OrderVO order = orderService.createOrder(userId, request);
        return Result.success(order);
    }

    @Operation(summary = "获取订单列表")
    @GetMapping
    public Result<PageResult<OrderVO>> getOrderList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtils.getCurrentUserId();
        PageResult<OrderVO> result = orderService.getOrderList(userId, status, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        OrderVO order = orderService.getOrderDetail(userId, id);
        return Result.success(order);
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        orderService.cancelOrder(userId, id);
        return Result.success("取消成功", null);
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirmOrder(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        orderService.confirmOrder(userId, id);
        return Result.success("确认收货成功", null);
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        orderService.deleteOrder(userId, id);
        return Result.success("删除成功", null);
    }
}
