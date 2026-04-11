package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.config.OrderTimeoutProperties;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
/**
 * 提供给内部服务调用的订单超时关单接口。
 */
public class OrderTimeoutInternalController {

    private final OrderTimeoutService orderTimeoutService;
    private final OrderTimeoutProperties properties;

    /**
     * 由 mq-consumer 调用，用于执行超时关单。
     */
    @PostMapping("/{id}/timeout-close")
    public Result<Map<String, Object>> closeExpiredOrder(
            @PathVariable Long id,
            @RequestHeader(value = Constants.INTERNAL_TOKEN_HEADER, required = false) String token) {
        validateToken(token);
        boolean closed = orderTimeoutService.closeExpiredOrder(id);
        return Result.success(Map.of("closed", closed, "orderId", id));
    }

    /**
     * 通过内部 token 做轻量鉴权，避免外部直接调用。
     */
    private void validateToken(String token) {
        if (!StringUtils.hasText(token) || !properties.getInternalApiToken().equals(token)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
