package com.example.commerce.inventory.application;

import com.example.commerce.commons.amqp.EventChannels;
import com.example.commerce.commons.events.DomainEvent;
import com.example.commerce.commons.events.InventoryFailed;
import com.example.commerce.commons.events.InventoryReserved;
import com.example.commerce.commons.events.OrderCreated;
import com.example.commerce.commons.logging.OrderIdMdc;
import com.example.commerce.commons.metrics.CommerceMetered;
import com.example.commerce.commons.metrics.MeteredOutcomes;
import com.example.commerce.inventory.domain.OutOfStockException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class ReserveOrderCreatedConsumer {

  private static final Logger monitoring = LoggerFactory.getLogger("monitoring");

  private final InventoryReservationWorkflow workflow;
  private final StreamBridge streamBridge;
  private final Counter reservedCounter;
  private final Counter failedCounter;

  public ReserveOrderCreatedConsumer(
      InventoryReservationWorkflow workflow,
      StreamBridge streamBridge,
      MeterRegistry meterRegistry) {
    this.workflow = workflow;
    this.streamBridge = streamBridge;
    this.reservedCounter =
        Counter.builder("inventory.reserved.count")
            .description("Number of successful stock reservations")
            .register(meterRegistry);
    this.failedCounter =
        Counter.builder("inventory.failed.count")
            .description("Number of failed stock reservations")
            .register(meterRegistry);
  }

  /**
   * Spring Cloud Stream handler body (invoked via {@code Consumer<OrderCreated>} bean). Uses {@link
   * CommerceMetered} so the proxy sees the annotation (lambda beans are not advised).
   */
  @CommerceMetered("consume.order.created")
  public void accept(OrderCreated event) {
    OrderIdMdc.run(
        event.orderId(),
        () -> {
          MeteredOutcomes.highCardinalityTag("commerce.order.id", event.orderId().toString());

          var maybeOutcome = workflow.completeReservation(event);
          if (maybeOutcome.isEmpty()) {
            MeteredOutcomes.outcome("skipped");
            return;
          }
          DomainEvent result = maybeOutcome.orElseThrow();

          if (result instanceof InventoryReserved) {
            reservedCounter.increment();
            MeteredOutcomes.event("inventory.reserved");
            monitoring.info(
                "action=INVENTORY_RESERVED orderId={} lines={}",
                event.orderId(),
                event.lines().size());
          } else if (result instanceof InventoryFailed failed) {
            failedCounter.increment();
            MeteredOutcomes.error(new OutOfStockException(failed.reason()));
            monitoring.error(
                "action=INVENTORY_FAILED orderId={} reason={}",
                event.orderId(),
                failed.reason());
          }

          final DomainEvent outEvent = result;
          streamBridge.send(
              EventChannels.OUTPUT_BINDING,
              MessageBuilder.withPayload(outEvent)
                  .setHeader(EventChannels.ROUTING_KEY_HEADER, outEvent.routingKey())
                  .build());
        });
  }
}
