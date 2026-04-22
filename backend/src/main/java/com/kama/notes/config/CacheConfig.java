package com.kama.notes.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置类
 */
@Configuration
public class CacheConfig {

    /**
     * 笔记详情本地缓存（Caffeine L1）
     * 最大容量 5000，写入后 10 分钟过期，开启缓存统计
     */
    @Bean
    public Cache<String, Object> noteLocalCache() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}
