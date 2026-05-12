package com.example.commerce.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.commerce.commons.metrics.CommerceMeteredAspect;
import com.example.commerce.commons.metrics.CommerceMeterNames;
import com.example.commerce.order.persistence.OrderEntity;
import com.example.commerce.order.persistence.OrderRepository;
import com.example.commerce.order.web.OrderResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class OrderReadServiceTest {

  @Mock OrderRepository repository;
  @Mock CacheManager cacheManager;
  @Mock Cache cache;

  SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  ObservationRegistry observationRegistry = ObservationRegistry.create();

  private static final CommerceMeterNames METER_NAMES =
      CommerceMeterNames.fromSpringApplicationName("order-service");
  private static final String DETAIL_CACHE_METRIC =
      METER_NAMES.prefix("order.detail.cache");

  OrderReadService orderReadService;

  @BeforeEach
  void setUp() {
    when(cacheManager.getCache(OrderReadService.CACHE_NAME)).thenReturn(cache);
    OrderReadService target = new OrderReadService(repository, cacheManager);
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    factory.addAspect(
        new CommerceMeteredAspect(meterRegistry, observationRegistry, METER_NAMES));
    orderReadService = factory.getProxy();
  }

  @Test
  void cache_hit_counts_hit_and_skips_repository() {
    UUID id = UUID.randomUUID();
    OrderResponse cached = new OrderResponse(id, "CONFIRMED");
    Cache.ValueWrapper wrapper = Mockito.mock(Cache.ValueWrapper.class);
    when(wrapper.get()).thenReturn(cached);
    when(cache.get(id)).thenReturn(wrapper);

    OrderResponse result = orderReadService.loadOrderDetail(id);

    assertThat(result).isEqualTo(cached);
    verify(repository, never()).findById(any());
    verify(cache, never()).put(any(), any());
    assertThat(meterRegistry.counter(DETAIL_CACHE_METRIC, "result", "hit").count())
        .isEqualTo(1);
    assertThat(meterRegistry.counter(DETAIL_CACHE_METRIC, "result", "miss").count())
        .isZero();
  }

  @Test
  void cache_miss_pending_does_not_put_cache_and_counts_miss() {
    OrderEntity entity = OrderEntity.pending("x");
    UUID id = entity.id();
    when(cache.get(id)).thenReturn(null);
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    OrderResponse result = orderReadService.loadOrderDetail(id);

    assertThat(result).isEqualTo(new OrderResponse(id, "PENDING"));
    verify(cache, never()).put(any(), any());
    assertThat(meterRegistry.counter(DETAIL_CACHE_METRIC, "result", "hit").count())
        .isZero();
    assertThat(meterRegistry.counter(DETAIL_CACHE_METRIC, "result", "miss").count())
        .isEqualTo(1);
  }

  @Test
  void cache_miss_confirmed_puts_cache_and_counts_miss() {
    OrderEntity entity = OrderEntity.pending("x");
    UUID id = entity.id();
    entity.confirm();
    when(cache.get(id)).thenReturn(null);
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    OrderResponse result = orderReadService.loadOrderDetail(id);

    assertThat(result.status()).isEqualTo("CONFIRMED");
    verify(cache).put(id, result);
    assertThat(meterRegistry.counter(DETAIL_CACHE_METRIC, "result", "miss").count())
        .isEqualTo(1);
  }

  @Test
  void cache_miss_not_found_does_not_put_and_counts_miss() {
    UUID id = UUID.randomUUID();
    when(cache.get(id)).thenReturn(null);
    when(repository.findById(id)).thenReturn(Optional.empty());

    OrderResponse result = orderReadService.loadOrderDetail(id);

    assertThat(result).isNull();
    verify(cache, never()).put(any(), any());
    assertThat(meterRegistry.counter(DETAIL_CACHE_METRIC, "result", "miss").count())
        .isEqualTo(1);
  }

  @Test
  void method_failure_propagates_and_skips_outcome_increments() {
    UUID id = UUID.randomUUID();
    Cache.ValueWrapper wrapper = Mockito.mock(Cache.ValueWrapper.class);
    when(wrapper.get()).thenReturn("not-an-OrderResponse");
    when(cache.get(id)).thenReturn(wrapper);

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, () -> orderReadService.loadOrderDetail(id));

    // The "hit" branch tagged the observation+counter before throwing; "miss" is unaffected.
    assertThat(meterRegistry.counter(DETAIL_CACHE_METRIC, "result", "hit").count())
        .isEqualTo(1);
    assertThat(meterRegistry.counter(DETAIL_CACHE_METRIC, "result", "miss").count())
        .isZero();
  }
}
