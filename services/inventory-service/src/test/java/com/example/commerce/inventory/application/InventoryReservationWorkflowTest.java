package com.example.commerce.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.commerce.commons.events.InventoryFailed;
import com.example.commerce.commons.events.OrderCreated;
import com.example.commerce.commons.events.OrderLine;
import com.example.commerce.inventory.persistence.ProcessedEventRepository;
import com.example.commerce.inventory.persistence.StockEntity;
import com.example.commerce.inventory.persistence.StockRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "spring.cloud.config.enabled=false",
  "spring.config.import=",
  "eureka.client.enabled=false",
  "spring.application.name=inventory-service",
  "spring.datasource.url=jdbc:h2:mem:testdb-inv-workflow",
  "spring.datasource.driver-class-name=org.h2.Driver",
  "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureObservability
class InventoryReservationWorkflowTest {

  @Autowired InventoryReservationWorkflow workflow;
  @Autowired StockRepository stockRepository;
  @Autowired ProcessedEventRepository processedEventRepository;

  @BeforeEach
  void seedStock() {
    processedEventRepository.deleteAll();
    stockRepository.deleteAll();
    stockRepository.save(new StockEntity("SKU-1", 10));
  }

  @Test
  void same_order_created_event_is_not_applied_twice() {
    UUID dedupeKey = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    var lines = List.of(new OrderLine("SKU-1", 1, BigDecimal.ONE));
    OrderCreated once =
        new OrderCreated(dedupeKey, orderId, "buyer", lines, Instant.now());

    assertThat(workflow.completeReservation(once)).isPresent();
    assertThat(workflow.completeReservation(once)).isEmpty();

    assertThat(stockRepository.findById("SKU-1")).hasValueSatisfying(s -> assertThat(s.getQuantity()).isEqualTo(9));
  }

  @Test
  void out_of_stock_returns_inventory_failed_and_commits_processed_marker() {
    UUID dedupeKey = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    var lines = List.of(new OrderLine("SKU-1", 100, BigDecimal.ONE));
    OrderCreated event =
        new OrderCreated(dedupeKey, orderId, "buyer", lines, Instant.now());

    var result = workflow.completeReservation(event);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow()).isInstanceOf(InventoryFailed.class);
    assertThat(processedEventRepository.existsByEventId(dedupeKey)).isTrue();
    assertThat(stockRepository.findById("SKU-1")).hasValueSatisfying(s -> assertThat(s.getQuantity()).isEqualTo(10));
  }
}
