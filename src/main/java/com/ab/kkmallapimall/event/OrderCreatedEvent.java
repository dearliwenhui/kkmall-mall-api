package com.ab.kkmallapimall.event;

import java.time.LocalDateTime;

/**
 * 订单创建完成后的领域事件。
 * 用于在事务提交后异步发送“超时检查”延时消息。
 */
public record OrderCreatedEvent(Long orderId, String orderNo, Long userId, LocalDateTime expireTime) {
}
