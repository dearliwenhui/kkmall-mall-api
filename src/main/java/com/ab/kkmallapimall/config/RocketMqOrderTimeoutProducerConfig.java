package com.ab.kkmallapimall.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OrderTimeoutProperties.class)
/**
 * 订单超时延时消息生产者配置。
 */
public class RocketMqOrderTimeoutProducerConfig {

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "kkmall.order-timeout.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DefaultMQProducer orderTimeoutProducer(OrderTimeoutProperties properties) {
        // 这里使用独立生产者组，避免和其他业务消息互相影响。
        DefaultMQProducer producer = new DefaultMQProducer(properties.resolveProducerGroup());
        producer.setNamesrvAddr(properties.getRocketmq().getNameserver());
        producer.setRetryTimesWhenSendFailed(2);
        producer.setSendMsgTimeout(properties.getRocketmq().getSendTimeoutMs());
        return producer;
    }
}
