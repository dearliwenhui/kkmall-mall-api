package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.config.HotProductProperties;
import com.ab.kkmallapimall.entity.Product;
import com.ab.kkmallapimall.mapper.ProductMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductProjectionCacheService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;
    private final HotProductProperties hotProductProperties;

    public Map<Long, Product> getProductsByIds(List<Long> ids) {
        Map<Long, Product> result = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(ids)) {
            return result;
        }

        Set<Long> missingIds = new LinkedHashSet<>();
        for (Long id : ids) {
            Product cached = getProductFromCache(id);
            if (cached != null && cached.getStatus() != null && cached.getStatus() == 1) {
                result.put(id, cached);
            } else {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Product> products = productMapper.selectBatchIds(missingIds).stream()
                    .filter(product -> product.getStatus() != null && product.getStatus() == 1)
                    .toList();
            for (Product product : products) {
                backfillProduct(product);
                result.put(product.getId(), product);
            }
        }

        return result;
    }

    public Product getProductById(Long id) {
        if (id == null) {
            return null;
        }
        Product cached = getProductFromCache(id);
        if (cached != null && cached.getStatus() != null && cached.getStatus() == 1) {
            return cached;
        }

        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            return null;
        }
        backfillProduct(product);
        return product;
    }

    private Product getProductFromCache(Long id) {
        try {
            Object cached = redisTemplate.opsForValue().get(hotProductProperties.productCacheKey(id));
            if (!(cached instanceof Map<?, ?> cachedMap)) {
                return null;
            }
            return snakeCaseMapper().convertValue(cachedMap, Product.class);
        } catch (Exception exception) {
            log.warn("Failed to read product projection cache. productId={}", id, exception);
            return null;
        }
    }

    private void backfillProduct(Product product) {
        try {
            String key = hotProductProperties.productCacheKey(product.getId());
            Long incomingVersion = product.getVersion();
            if (incomingVersion == null) {
                incomingVersion = product.getUpdateTime() == null
                        ? System.currentTimeMillis()
                        : product.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            Object current = redisTemplate.opsForValue().get(key);
            if (current instanceof Map<?, ?> currentMap) {
                Long currentVersion = toLong(currentMap.get("version"));
                if (currentVersion != null && currentVersion >= incomingVersion) {
                    return;
                }
            }
            Map<String, Object> payload = new LinkedHashMap<>(snakeCaseMapper().convertValue(product, MAP_TYPE));
            payload.put("version", incomingVersion);
            redisTemplate.opsForValue().set(key, payload);
        } catch (Exception exception) {
            log.warn("Failed to backfill product projection cache. productId={}", product.getId(), exception);
        }
    }

    private ObjectMapper snakeCaseMapper() {
        return objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
