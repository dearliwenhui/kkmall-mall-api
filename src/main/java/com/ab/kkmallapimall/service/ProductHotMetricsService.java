package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.config.HotProductProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductHotMetricsService {

    private final StringRedisTemplate stringRedisTemplate;
    private final HotProductProperties hotProductProperties;

    public void recordProductView(Long productId) {
        if (productId == null) {
            return;
        }
        try {
            String rankKey = hotProductProperties.todayRankKey();
            stringRedisTemplate.opsForZSet().incrementScore(rankKey, String.valueOf(productId), 1D);
            stringRedisTemplate.expire(rankKey, Duration.ofDays(hotProductProperties.getDailyRankTtlDays()));
        } catch (Exception exception) {
            log.warn("Failed to record product view. productId={}", productId, exception);
        }
    }

    public List<Long> getHotProductIds(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        Set<String> members = reverseRange(hotProductProperties.currentRankKey(), safeLimit);
        if (members == null || members.isEmpty()) {
            members = reverseRange(hotProductProperties.todayRankKey(), safeLimit);
        }
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        return members.stream()
                .map(this::parseLong)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public void refreshCurrentRanking() {
        try {
            String sourceKey = hotProductProperties.todayRankKey();
            Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(sourceKey, 0, hotProductProperties.getCurrentRankSize() - 1L);
            String currentKey = hotProductProperties.currentRankKey();
            stringRedisTemplate.delete(currentKey);
            if (tuples == null || tuples.isEmpty()) {
                return;
            }
            Set<ZSetOperations.TypedTuple<String>> normalized = new LinkedHashSet<>(tuples);
            stringRedisTemplate.opsForZSet().add(currentKey, normalized);
            stringRedisTemplate.expire(currentKey, Duration.ofHours(hotProductProperties.getCurrentRankTtlHours()));
        } catch (Exception exception) {
            log.warn("Failed to refresh hot product ranking", exception);
        }
    }

    private Set<String> reverseRange(String key, int safeLimit) {
        Set<String> values = stringRedisTemplate.opsForZSet().reverseRange(key, 0, safeLimit - 1L);
        return values == null ? Collections.emptySet() : values;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 10;
        }
        return Math.min(limit, 50);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            log.warn("Failed to parse hot product id={}", value, exception);
            return null;
        }
    }
}
