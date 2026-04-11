package com.ab.kkmallapimall.listener;

import com.ab.kkmallapimall.config.OrderTimeoutProperties;
import com.ab.kkmallapimall.event.OrderCreatedEvent;
import com.ab.kkmallapimall.model.OrderTimeoutCheckMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * 监听订单创建事件，并在事务提交成功后发送延时关单检查消息。
 */
public class OrderTimeoutMessagePublisher {

    private final ObjectMapper objectMapper;
    private final OrderTimeoutProperties properties;
    private final ObjectProvider<DefaultMQProducer> producerProvider;

    /**
     * 只有待支付订单才会进入这条链路。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (event.expireTime() == null || !properties.getRocketmq().isEnabled()) {
            return;
        }
        DefaultMQProducer producer = producerProvider.getIfAvailable();
        if (producer == null) {
            log.warn("Order timeout producer is disabled. orderId={}", event.orderId());
            return;
        }
        try {
            // 消息里带上订单号和过期时间，方便消费端幂等处理和排障。
            OrderTimeoutCheckMessage payload = new OrderTimeoutCheckMessage(
                    event.orderId(),
                    event.orderNo(),
                    event.userId(),
                    event.expireTime()
            );
            Message message = new Message(properties.resolveTopic(), objectMapper.writeValueAsBytes(payload));
            if (StringUtils.hasText(properties.getRocketmq().getTag())) {
                message.setTags(properties.getRocketmq().getTag());
            }
            message.setKeys(event.orderNo());
            message.putUserProperty("orderId", String.valueOf(event.orderId()));
            // 这里采用 RocketMQ 的固定延时级别，实际时长由配置控制。
            message.setDelayTimeLevel(properties.getRocketmq().getDelayLevel());
            SendResult sendResult = producer.send(message);
            log.info("Sent order timeout check message. orderId={}, orderNo={}, sendStatus={}",
                    event.orderId(), event.orderNo(), sendResult.getSendStatus());
        } catch (Exception exception) {
            log.error("Failed to send order timeout check message. orderId={}, orderNo={}",
                    event.orderId(), event.orderNo(), exception);
        }
    }
}
