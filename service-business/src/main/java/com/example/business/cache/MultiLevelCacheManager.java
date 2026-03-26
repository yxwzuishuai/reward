package com.example.business.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 多级缓存管理器实现
 * 支持缓存穿透防护（空值缓存）、缓存击穿防护（分布式锁）、缓存雪崩防护（随机TTL）
 */
@Slf4j
@Component
public class MultiLevelCacheManager implements MultiLevelCache {

    private static final String NULL_PLACEHOLDER = "@@NULL@@";
    private static final String LOCK_PREFIX = "lock:";

    private final Cache<String, String> caffeineCache;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cache.redis.base-ttl:600}")
    private long baseTtl;

    @Value("${cache.redis.random-ttl-range:120}")
    private long randomTtlRange;

    public MultiLevelCacheManager(Cache<String, String> caffeineCache,
                                   StringRedisTemplate redisTemplate,
                                   ObjectMapper objectMapper) {
        this.caffeineCache = caffeineCache;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <V> V get(String key, Function<String, V> dbLoader, Class<V> clazz) {
        // L1: 查本地缓存
        String cached = caffeineCache.getIfPresent(key);
        if (cached != null) {
            if (NULL_PLACEHOLDER.equals(cached)) return null;
            return deserialize(cached, clazz);
        }

        // L2: 查 Redis
        cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (NULL_PLACEHOLDER.equals(cached)) {
                caffeineCache.put(key, NULL_PLACEHOLDER);
                return null;
            }
            caffeineCache.put(key, cached);
            return deserialize(cached, clazz);
        }

        // DB 回源（简单锁防击穿）
        String lockKey = LOCK_PREFIX + key;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 3, TimeUnit.SECONDS);
        try {
            if (Boolean.TRUE.equals(locked)) {
                // 双重检查
                cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    caffeineCache.put(key, cached);
                    return NULL_PLACEHOLDER.equals(cached) ? null : deserialize(cached, clazz);
                }

                V value = dbLoader.apply(key);
                if (value != null) {
                    String json = serialize(value);
                    long ttl = baseTtl + ThreadLocalRandom.current().nextLong(randomTtlRange);
                    redisTemplate.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);
                    caffeineCache.put(key, json);
                } else {
                    // 空值缓存防穿透
                    redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, 60, TimeUnit.SECONDS);
                    caffeineCache.put(key, NULL_PLACEHOLDER);
                }
                return value;
            }
        } finally {
            if (Boolean.TRUE.equals(locked)) {
                redisTemplate.delete(lockKey);
            }
        }

        // 未获取到锁，直接查 DB（降级）
        return dbLoader.apply(key);
    }

    @Override
    public void put(String key, Object value) {
        String json = serialize(value);
        long ttl = baseTtl + ThreadLocalRandom.current().nextLong(randomTtlRange);
        redisTemplate.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);
        caffeineCache.put(key, json);
    }

    @Override
    public void evict(String key) {
        redisTemplate.delete(key);
        caffeineCache.invalidate(key);
    }

    @Override
    public void evictByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            keys.forEach(caffeineCache::invalidate);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("缓存序列化失败", e);
        }
    }

    private <V> V deserialize(String json, Class<V> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("缓存反序列化失败", e);
        }
    }
}
