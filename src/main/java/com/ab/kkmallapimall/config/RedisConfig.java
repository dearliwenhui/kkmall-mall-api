package com.ab.kkmallapimall.config;

import com.ab.kkmallapimall.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration.
 */
@Slf4j
@Configuration
public class RedisConfig implements CachingConfigurer {

    @Value("${spring.data.redis.key-prefix:kkmall-mall-api}")
    private String redisKeyPrefix;

    @Value("${kkmall.cache.product.list-ttl:5m}")
    private Duration productListCacheTtl;

    @Value("${kkmall.cache.product.hot-ttl:5m}")
    private Duration productHotCacheTtl;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .computePrefixWith(cacheName -> redisKeyPrefix + "::" + cacheName + "::")
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(
                Constants.Cache.PRODUCT_LIST,
                defaultConfig.entryTtl(productListCacheTtl)
        );
        cacheConfigurations.put(
                Constants.Cache.PRODUCT_HOT,
                defaultConfig.entryTtl(productHotCacheTtl)
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean("productListCacheKeyGenerator")
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            Long categoryId = params.length > 0 ? (Long) params[0] : null;
            String keyword = params.length > 1 ? normalize((String) params[1], false) : "";
            String sortBy = params.length > 2 ? normalize((String) params[2], true) : "";
            Integer pageNum = params.length > 3 ? (Integer) params[3] : null;
            Integer pageSize = params.length > 4 ? (Integer) params[4] : null;

            return "categoryId=" + (categoryId == null ? "null" : categoryId)
                    + ":keyword=" + keyword
                    + ":sortBy=" + sortBy
                    + ":pageNum=" + pageNum
                    + ":pageSize=" + pageSize;
        };
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache get failed. cache={}, key={}", cache == null ? null : cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis cache put failed. cache={}, key={}", cache == null ? null : cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache evict failed. cache={}, key={}", cache == null ? null : cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis cache clear failed. cache={}", cache == null ? null : cache.getName(), exception);
            }
        };
    }

    @Bean("hotProductsCacheKeyGenerator")
    public KeyGenerator hotProductsCacheKeyGenerator() {
        return (target, method, params) -> {
            Integer limit = params.length > 0 ? (Integer) params[0] : null;
            int normalizedLimit = normalizeHotLimit(limit);
            return "limit=" + normalizedLimit;
        };
    }

    private String normalize(String value, boolean lowerCase) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        return lowerCase ? normalized.toLowerCase() : normalized;
    }

    private int normalizeHotLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 10;
        }
        return Math.min(limit, 50);
    }
}
