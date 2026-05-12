package com.example.commerce.order.application;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class OrderCacheEvictor {

  @CacheEvict(cacheNames = "orders", key = "#orderId")
  public void evict(UUID orderId) {
    /* Eviction is driven by the annotation; body intentionally empty. */
  }
}
