package com.ab.kkmallapimall.scheduler;

import com.ab.kkmallapimall.service.ProductHotMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotProductRankingScheduler {

    private final ProductHotMetricsService productHotMetricsService;

    @Scheduled(cron = "${kkmall.hot-product.refresh-cron:0 */5 * * * *}")
    public void refreshCurrentRank() {
        productHotMetricsService.refreshCurrentRanking();
    }
}
