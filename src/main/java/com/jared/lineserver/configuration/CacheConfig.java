package com.jared.lineserver.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public final static String CACHE_ONE = "cacheOne";

    @Bean
    public Cache cacheOne() {
        return new CaffeineCache(CACHE_ONE, Caffeine.newBuilder()
                .maximumSize(1000)
                .build());
    }

}