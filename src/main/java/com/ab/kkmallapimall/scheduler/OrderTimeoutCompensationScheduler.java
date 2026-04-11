package com.ab.kkmallapimall.scheduler;

import com.ab.kkmallapimall.config.OrderTimeoutProperties;
import com.ab.kkmallapimall.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * 超时关单补偿任务。
 * RocketMQ 延时消息是主链路，这里负责兜底。
 */
public class OrderTimeoutCompensationScheduler {

    private final OrderTimeoutService orderTimeoutService;
    private final OrderTimeoutProperties properties;

    @Scheduled(cron = "${kkmall.order-timeout.compensation-cron:0 */1 * * * *}")
    public void closeExpiredOrders() {
        int closedCount = orderTimeoutService.closeExpiredOrders(properties.getBatchSize());
        if (closedCount > 0) {
            log.info("Closed {} expired orders via compensation scheduler", closedCount);
        }
    }
}
