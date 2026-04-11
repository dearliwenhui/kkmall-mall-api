package com.ab.kkmallapimall.model;

import java.time.LocalDateTime;

/**
 * 发给 RocketMQ 的订单超时检查消息体。
 */
public record OrderTimeoutCheckMessage(Long orderId, String orderNo, Long userId, LocalDateTime expireTime) {
}
