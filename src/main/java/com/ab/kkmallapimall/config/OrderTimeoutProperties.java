package com.ab.kkmallapimall.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "kkmall.order-timeout")
/**
 * 订单超时能力的统一配置。
 */
public class OrderTimeoutProperties {

    @NotBlank
    private String env = "dev";

    @Min(1)
    private int timeoutMinutes = 30;

    @Min(1)
    private int batchSize = 100;

    @NotBlank
    private String compensationCron = "0 */1 * * * *";

    @NotBlank
    private String internalApiToken = "kkmall-order-timeout-dev-token";

    private final RocketMq rocketmq = new RocketMq();

    @Data
    /**
     * RocketMQ 相关配置。
     */
    public static class RocketMq {

        private boolean enabled = true;

        @NotBlank
        private String nameserver = "rocketmq-nameserver.rocketmq.svc.cluster.local:9876";

        @NotBlank
        private String producerGroup = "kkmall-mall-api-order-timeout-producer";

        @NotBlank
        private String topic = "kkmall_order_timeout";

        private String tag = "check";

        @Min(1)
        private int delayLevel = 17;

        @Min(1000)
        private int sendTimeoutMs = 3000;
    }

    /**
     * 解析带环境前缀的 Topic 名称，例如 dev_kkmall_order_timeout。
     */
    public String resolveTopic() {
        return applyEnvPrefix(rocketmq.getTopic());
    }

    /**
     * 解析带环境后缀的 Producer Group，例如 kkmall-mall-api-order-timeout-producer-dev。
     */
    public String resolveProducerGroup() {
        return applyEnvSuffix(rocketmq.getProducerGroup());
    }

    private String applyEnvPrefix(String value) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(env)) {
            return value;
        }
        String normalizedEnv = env.trim().toLowerCase();
        String prefix = normalizedEnv + "_";
        return value.startsWith(prefix) ? value : prefix + value;
    }

    private String applyEnvSuffix(String value) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(env)) {
            return value;
        }
        String normalizedEnv = env.trim().toLowerCase();
        String suffix = "-" + normalizedEnv;
        return value.endsWith(suffix) ? value : value + suffix;
    }
}
