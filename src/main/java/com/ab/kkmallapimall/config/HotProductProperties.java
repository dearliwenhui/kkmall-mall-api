package com.ab.kkmallapimall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "kkmall.hot-product")
public class
HotProductProperties {

    private String env = "dev";
    private String dailyRankKeyPrefix = "hot:product:rank";
    private String currentRankKeyPrefix = "hot:product:rank:current";
    private int currentRankSize = 100;
    private int currentRankTtlHours = 48;
    private int dailyRankTtlDays = 8;
    private String refreshCron = "0 */5 * * * *";
    private String cdcCacheKeyPrefix = "cdc";

    public String todayRankKey() {
        return dailyRankKeyPrefix + ":" + env + ":" + java.time.LocalDate.now();
    }

    public String currentRankKey() {
        return currentRankKeyPrefix + ":" + env;
    }

    public String productCacheKey(Long productId) {
        return cdcCacheKeyPrefix + ":" + env + ":mall_product:" + productId;
    }
}
