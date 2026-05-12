package com.example.commerce.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.commerce.commons.metrics.CommerceMeteredAspect;
import com.example.commerce.commons.metrics.CommerceMeterNames;
import com.example.commerce.order.persistence.OrderRepository;
import com.example.commerce.order.web.OrderResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceLookupTest {

  static final String LOOKUP_METRIC = "commerce.order_service.order.lookup";

  @Mock OrderRepository repository;
  @Mock OrderReadService orderReadService;

  SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  ObservationRegistry observationRegistry = ObservationRegistry.create();
  CommerceMeterNames meterNames = CommerceMeterNames.fromSpringApplicationName("order-service");

  PlaceOrderService placeOrderService;

  @BeforeEach
  void setUp() {
    // StreamBridge is final (hard to mock without mockito-inline); find() never uses it.
    PlaceOrderService target =
        new PlaceOrderService(repository, orderReadService, null, meterRegistry);
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    factory.addAspect(new CommerceMeteredAspect(meterRegistry, observationRegistry, meterNames));
    placeOrderService = factory.getProxy();
  }

  @Test
  void find_existing_order_counts_found() {
    UUID id = UUID.randomUUID();
    OrderResponse body = new OrderResponse(id, "PENDING");
    when(orderReadService.loadOrderDetail(id)).thenReturn(body);

    assertThat(placeOrderService.find(id)).isEqualTo(body);
    assertThat(meterRegistry.counter(LOOKUP_METRIC, "result", "found").count()).isEqualTo(1);
    assertThat(meterRegistry.counter(LOOKUP_METRIC, "result", "not_found").count()).isZero();
  }

  @Test
  void find_missing_order_counts_not_found_and_throws() {
    UUID id = UUID.randomUUID();
    when(orderReadService.loadOrderDetail(id)).thenReturn(null);

    assertThatThrownBy(() -> placeOrderService.find(id)).isInstanceOf(NoSuchElementException.class);

    assertThat(meterRegistry.counter(LOOKUP_METRIC, "result", "not_found").count()).isEqualTo(1);
    assertThat(meterRegistry.counter(LOOKUP_METRIC, "result", "found").count()).isZero();
  }
}
