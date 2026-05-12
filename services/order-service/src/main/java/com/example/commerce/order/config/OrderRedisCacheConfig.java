package com.example.commerce.order.config;

import com.example.commerce.order.web.OrderResponse;
import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@Profile("!test")
public class OrderRedisCacheConfig {

  @Bean
  RedisCacheManagerBuilderCustomizer ordersRedisCacheCustomizer() {
    Jackson2JsonRedisSerializer<OrderResponse> serializer =
        new Jackson2JsonRedisSerializer<>(OrderResponse.class);
    RedisCacheConfiguration cfg = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .disableCachingNullValues()
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    return builder -> builder.withCacheConfiguration("orders", cfg);
  }
}
