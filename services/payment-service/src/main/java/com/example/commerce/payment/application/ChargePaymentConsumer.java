package com.example.commerce.payment.application;

import com.example.commerce.commons.amqp.EventChannels;
import com.example.commerce.commons.events.DomainEvent;
import com.example.commerce.commons.events.InventoryReserved;
import com.example.commerce.commons.events.PaymentCompleted;
import com.example.commerce.commons.events.PaymentFailed;
import com.example.commerce.commons.logging.OrderIdMdc;
import com.example.commerce.commons.metrics.CommerceMetered;
import com.example.commerce.commons.metrics.MeteredOutcomes;
import com.example.commerce.payment.domain.PaymentDeclinedException;
import com.example.commerce.payment.persistence.ProcessedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class ChargePaymentConsumer {

  private static final Logger monitoring = LoggerFactory.getLogger("monitoring");

  private final PaymentGateway gateway;
  private final ProcessedEventRepository processed;
  private final StreamBridge streamBridge;
  private final Counter successCounter;
  private final Counter failedCounter;

  public ChargePaymentConsumer(
      PaymentGateway gateway,
      ProcessedEventRepository processed,
      StreamBridge streamBridge,
      MeterRegistry meterRegistry) {
    this.gateway = gateway;
    this.processed = processed;
    this.streamBridge = streamBridge;
    this.successCounter =
        Counter.builder("payments.successful.count")
            .description("Number of successful payments")
            .register(meterRegistry);
    this.failedCounter =
        Counter.builder("payments.failed.count")
            .description("Number of failed payments")
            .register(meterRegistry);
  }

  @CommerceMetered("payment.charge")
  public void accept(InventoryReserved event) {
    if (processed.existsByEventId(event.eventId())) {
      MeteredOutcomes.outcome("skipped", "duplicate_event");
      return;
    }

    OrderIdMdc.run(
        event.orderId(),
        () -> {
          MeteredOutcomes.lowCardinalityTag("commerce.order.id", event.orderId().toString());

          DomainEvent result;

          try {
            String txId = gateway.charge(event.orderId());
            result =
                new PaymentCompleted(UUID.randomUUID(), event.orderId(), txId, Instant.now());
            successCounter.increment();
            MeteredOutcomes.highCardinalityTag("commerce.payment.transaction_id", txId);
            MeteredOutcomes.event("payment.charged");
            MeteredOutcomes.outcome("charged");
            monitoring.info(
                "action=PAYMENT_CHARGED orderId={} transactionId={}",
                event.orderId(),
                txId);
          } catch (PaymentDeclinedException ex) {
            result =
                new PaymentFailed(
                    UUID.randomUUID(), event.orderId(), ex.getMessage(), Instant.now());
            failedCounter.increment();
            MeteredOutcomes.error(ex);
            MeteredOutcomes.outcome("declined");
            monitoring.warn(
                "action=PAYMENT_DECLINED orderId={} reason={}",
                event.orderId(),
                ex.getMessage());
          }

          final DomainEvent outEvent = result;
          streamBridge.send(
              EventChannels.OUTPUT_BINDING,
              MessageBuilder.withPayload(outEvent)
                  .setHeader(EventChannels.ROUTING_KEY_HEADER, outEvent.routingKey())
                  .build());

          processed.markProcessed(event.eventId());
        });
  }
}
