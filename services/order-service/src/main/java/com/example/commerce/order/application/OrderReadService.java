package com.example.commerce.order.application;

import com.example.commerce.commons.metrics.CommerceMetered;
import com.example.commerce.commons.metrics.MeteredOutcomes;
import com.example.commerce.order.persistence.OrderRepository;
import com.example.commerce.order.web.OrderResponse;
import java.util.UUID;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class OrderReadService {

  static final String CACHE_NAME = "orders";

  private final OrderRepository repository;
  private final CacheManager cacheManager;

  public OrderReadService(OrderRepository repository, CacheManager cacheManager) {
    this.repository = repository;
    this.cacheManager = cacheManager;
  }

  /**
   * Loads order detail from Postgres; hot values are stored in Redis under {@code orders} (see
   * {@link com.example.commerce.order.config.OrderRedisCacheConfig}). Missing orders ({@code null})
   * are not cached.
   *
   * <p><strong>Observability:</strong> {@link CommerceMetered} wraps this method in an Observation
   * named {@code commerce.order_service.order.detail.cache} (service segment from {@code
   * spring.application.name}) and logs failures at WARN. Each branch below calls {@link
   * MeteredOutcomes#outcome(String)} to attach {@code result=hit|miss} to the observation and
   * increment the matching Micrometer counter — Prometheus exposes it as {@code
   * commerce_order_service_order_detail_cache_total}. The global {@code application} tag is still
   * added via {@code management.metrics.tags}. Spring Data Redis may still emit
   * {@code cache_gets_total}.
   *
   * <p><strong>Pending orders are not cached:</strong> UIs poll {@code GET /orders/{id}} until the
   * saga completes. Caching {@code PENDING} would return a stale snapshot after the row becomes
   * {@code CONFIRMED} or {@code FAILED} until eviction/TTL.
   */
  @CommerceMetered("order.detail.cache")
  public OrderResponse loadOrderDetail(UUID id) {
    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache != null) {
      Cache.ValueWrapper cached = cache.get(id);
      if (cached != null) {
        MeteredOutcomes.outcome("hit");
        Object body = cached.get();
        if (!(body instanceof OrderResponse response)) {
          throw new IllegalStateException(
              "Unexpected cache entry type for orders cache: "
                  + (body == null ? "null" : body.getClass().getName()));
        }
        return response;
      }
    }

    MeteredOutcomes.outcome("miss");

    OrderResponse loaded =
        repository
            .findById(id)
            .map(o -> new OrderResponse(o.id(), o.status().name()))
            .orElse(null);

    if (loaded != null && cache != null && isTerminalStatus(loaded.status())) {
      cache.put(id, loaded);
    }
    return loaded;
  }

  private static boolean isTerminalStatus(String status) {
    return status != null && !"PENDING".equals(status);
  }
}
