package com.example.commerce.inventory.application;

import com.example.commerce.commons.events.DomainEvent;
import com.example.commerce.commons.events.InventoryFailed;
import com.example.commerce.commons.events.InventoryReserved;
import com.example.commerce.commons.events.OrderCreated;
import com.example.commerce.commons.metrics.CommerceMetered;
import com.example.commerce.commons.metrics.MeteredOutcomes;
import com.example.commerce.inventory.domain.OutOfStockException;
import com.example.commerce.inventory.persistence.ProcessedEventRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReservationWorkflow {

  private final StockService stockService;
  private final ProcessedEventRepository processedEventRepository;

  public InventoryReservationWorkflow(
      StockService stockService, ProcessedEventRepository processedEventRepository) {
    this.stockService = stockService;
    this.processedEventRepository = processedEventRepository;
  }

  /**
   * Idempotent reservation: stock deduction and processed-event marker commit together so a broker
   * redelivery cannot reserve twice for the same {@link OrderCreated#eventId()}.
   *
   * <p><strong>Observability:</strong> wrapped by {@link CommerceMetered}; publishes outcome counters
   * {@code commerce.inventory_service.inventory.reservation} with {@code result=duplicate|failed|reserved}
   * (parallel to legacy {@code inventory.reserved.count} / {@code inventory.failed.count} in the
   * consumer).
   *
   * @return empty when this {@code order.created} event was already handled; otherwise the event to
   *     publish downstream
   */
  @CommerceMetered("inventory.reservation")
  @Transactional
  public Optional<DomainEvent> completeReservation(OrderCreated event) {
    MeteredOutcomes.highCardinalityTag("commerce.order.id", event.orderId().toString());
    if (processedEventRepository.existsByEventId(event.eventId())) {
      MeteredOutcomes.outcome("duplicate");
      return Optional.empty();
    }
    try {
      stockService.reserveAll(event.orderId(), event.lines());
    } catch (OutOfStockException ex) {
      processedEventRepository.markProcessed(event.eventId());
      MeteredOutcomes.outcome("failed");
      return Optional.of(
          new InventoryFailed(UUID.randomUUID(), event.orderId(), ex.getMessage(), Instant.now()));
    }
    processedEventRepository.markProcessed(event.eventId());
    MeteredOutcomes.outcome("reserved");
    return Optional.of(
        new InventoryReserved(UUID.randomUUID(), event.orderId(), Instant.now()));
  }
}
