package com.example.business.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 多级缓存配置
 */
@Configuration
public class CacheConfig {

    @Value("${cache.caffeine.maximum-size:10000}")
    private long caffeineMaxSize;

    @Value("${cache.caffeine.expire-after-write:300}")
    private long caffeineExpireSeconds;

    @Bean
    public Cache<String, String> caffeineCache() {
        return Caffeine.newBuilder()
                .maximumSize(caffeineMaxSize)
                .expireAfterWrite(caffeineExpireSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build();
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
