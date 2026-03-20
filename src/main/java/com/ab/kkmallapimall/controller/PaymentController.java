package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.CreatePaymentRequest;
import com.ab.kkmallapimall.dto.PaymentVO;
import com.ab.kkmallapimall.service.PaymentService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付控制器
 */
@Tag(name = "支付管理", description = "支付相关接口")
@RestController
@RequestMapping("/api/mall/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "创建支付")
    @PostMapping
    public Result<PaymentVO> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        PaymentVO payment = paymentService.createPayment(userId, request);
        return Result.success(payment);
    }

    @Operation(summary = "处理支付（模拟支付）")
    @PostMapping("/{paymentNo}/process")
    public Result<Map<String, Object>> processPayment(@PathVariable String paymentNo) {
        Long userId = securityUtils.getCurrentUserId();
        Map<String, Object> result = paymentService.processPayment(userId, paymentNo);
        return Result.success(result);
    }

    @Operation(summary = "根据订单ID查询支付记录")
    @GetMapping("/order/{orderId}")
    public Result<PaymentVO> getPaymentByOrderId(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        PaymentVO payment = paymentService.getPaymentByOrderId(userId, orderId);
        return Result.success(payment);
    }

    @Operation(summary = "根据支付流水号查询")
    @GetMapping("/{paymentNo}")
    public Result<PaymentVO> getPaymentByNo(@PathVariable String paymentNo) {
        Long userId = securityUtils.getCurrentUserId();
        PaymentVO payment = paymentService.getPaymentByNo(userId, paymentNo);
        return Result.success(payment);
    }
}
