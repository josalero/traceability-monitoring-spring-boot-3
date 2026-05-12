# Inventory service

> **inventory-service** — consumes `order.created`, reserves stock with idempotency, publishes `inventory.reserved` / `inventory.failed`.
>
> [← Back to architecture overview](../ARCHITECTURE.md)

## 13. Inventory service — Stream Consumer with idempotency

The handler is a `Consumer<Message<OrderCreated>>` `@Bean`. The binder
binds it to the topic exchange `commerce.events` with routing key
`order.created`, creates the queue
`commerce.events.inventory-service`, and provisions a dedicated DLQ.

```java
package com.example.commerce.inventory.config;

import com.example.commerce.commons.amqp.EventChannels;
import com.example.commerce.commons.events.*;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

@Configuration
public class ReserveInventoryFunctions {

  @Bean
  public Consumer<Message<OrderCreated>> reserveInventory(
      StockService stock,
      ProcessedEventRepository processed,
      StreamBridge streamBridge) {

    return message -> {
      OrderCreated event = message.getPayload();
      if (processed.existsByEventId(event.eventId())) return;

      DomainEvent result;
      try {
        stock.reserveAll(event.lines());
        result = new InventoryReserved(UUID.randomUUID(), event.orderId(), Instant.now());
      } catch (OutOfStockException ex) {
        result = new InventoryFailed(UUID.randomUUID(), event.orderId(),
            ex.getMessage(), Instant.now());
      }

      streamBridge.send(EventChannels.OUTPUT_BINDING,
          MessageBuilder.withPayload(result)
              .setHeader(EventChannels.ROUTING_KEY_HEADER, result.routingKey())
              .build());

      processed.markProcessed(event.eventId());
    };
  }
}
```

**Inventory service `application.yml`** — function definition + binder
config. Prefetch, retry, and DLQ are binder properties on the binding,
not `spring.rabbitmq.listener.*`:

```yaml
spring:
  application:
    name: inventory-service
  cloud:
    function:
      definition: reserveInventory
    stream:
      bindings:
        reserveInventory-in-0:
          destination: commerce.events
          group: inventory-service
          consumer:
            maxAttempts: 3
            backOffInitialInterval: 1000
            backOffMultiplier: 2.0
            concurrency: 1
      rabbit:
        bindings:
          reserveInventory-in-0:
            consumer:
              exchangeType: topic
              bindingRoutingKey: order.created
              autoBindDlq: true
              republishToDlq: true
              prefetch: 10
              acknowledgeMode: AUTO
              durableSubscription: true
```

> `maxAttempts: 3` + `republishToDlq: true` means: on listener exception
> the binder retries 3 times with exponential backoff inside the same
> consumer, then **republishes** the message to
> `commerce.events.inventory-service.dlq` instead of nack-requeue. The
> idempotency check protects against duplicates on in-process retry. Each
> binding gets its own DLQ, so a poison message in `payment-service`
> never blocks `inventory-service`.

---

