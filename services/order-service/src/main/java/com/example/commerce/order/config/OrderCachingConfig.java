package com.example.commerce.order.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Enables Spring Cache processing for {@code @CacheEvict} and programmatic {@link org.springframework.cache.CacheManager} use in all profiles (tests use in-memory cache). */
@Configuration
@EnableCaching
public class OrderCachingConfig {}
