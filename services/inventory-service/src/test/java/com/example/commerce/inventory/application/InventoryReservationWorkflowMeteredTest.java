package com.example.commerce.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.commerce.commons.events.OrderCreated;
import com.example.commerce.commons.events.OrderLine;
import com.example.commerce.commons.metrics.CommerceMeteredAspect;
import com.example.commerce.commons.metrics.CommerceMeterNames;
import com.example.commerce.inventory.persistence.ProcessedEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

/** Unit-test outcomes with an explicit {@link CommerceMeteredAspect} (same wiring as Spring auto-config). */
@ExtendWith(MockitoExtension.class)
class InventoryReservationWorkflowMeteredTest {

  static final String RESERVATION_METRIC = "commerce.inventory_service.inventory.reservation";

  @Mock StockService stockService;
  @Mock ProcessedEventRepository processedEventRepository;

  SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  ObservationRegistry observationRegistry = ObservationRegistry.create();
  CommerceMeterNames meterNames = CommerceMeterNames.fromSpringApplicationName("inventory-service");

  InventoryReservationWorkflow workflow;

  @BeforeEach
  void setUp() {
    InventoryReservationWorkflow target =
        new InventoryReservationWorkflow(stockService, processedEventRepository);
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    factory.addAspect(new CommerceMeteredAspect(meterRegistry, observationRegistry, meterNames));
    workflow = factory.getProxy();
  }

  @Test
  void duplicate_event_counts_duplicate_outcome_only() {
    UUID dedupeKey = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    var lines = List.of(new OrderLine("SKU-1", 1, BigDecimal.ONE));
    OrderCreated event = new OrderCreated(dedupeKey, orderId, "buyer", lines, Instant.now());

    when(processedEventRepository.existsByEventId(dedupeKey)).thenReturn(true);

    assertThat(workflow.completeReservation(event)).isEmpty();

    assertThat(meterRegistry.counter(RESERVATION_METRIC, "result", "duplicate").count())
        .isEqualTo(1);
    assertThat(meterRegistry.counter(RESERVATION_METRIC, "result", "reserved").count())
        .isZero();
    verify(processedEventRepository).existsByEventId(dedupeKey);
  }

  @Test
  void success_counts_reserved_outcome() {
    UUID dedupeKey = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    var lines = List.of(new OrderLine("SKU-1", 1, BigDecimal.ONE));
    OrderCreated event = new OrderCreated(dedupeKey, orderId, "buyer", lines, Instant.now());

    when(processedEventRepository.existsByEventId(dedupeKey)).thenReturn(false);

    assertThat(workflow.completeReservation(event)).isPresent();

    assertThat(meterRegistry.counter(RESERVATION_METRIC, "result", "reserved").count())
        .isEqualTo(1);
    verify(stockService).reserveAll(orderId, lines);
    verify(processedEventRepository).markProcessed(dedupeKey);
  }
}
