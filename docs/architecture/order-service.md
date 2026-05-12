# Order service

> **order-service** — REST API, Redis-backed order reads, `PlaceOrderService`, Stream producers, and saga consumers that confirm or fail orders.
>
> [← Back to architecture overview](../ARCHITECTURE.md)

## Redis cache (order detail reads)

**order-service** uses **Spring Cache** backed by **Redis** for `GET /orders/{id}`:

- **Hit:** response served from Redis (no Postgres query) — only **terminal** statuses (**`CONFIRMED`**, **`FAILED`**) are cached.
- **Miss:** row loaded from Postgres. **`PENDING`** responses are **not** written to Redis so polling UIs always see fresh saga progress; **`CONFIRMED`** / **`FAILED`** are stored with TTL (**10 minutes**, see `config-repo/order-service.yml`).
- **Eviction:** after an order row is updated (confirm / fail handlers), the corresponding cache entry is removed so the next read refreshes from Postgres.

**Metrics:** `loadOrderDetail` is decorated with the reusable **`@CommerceMetered("order.detail.cache")`** annotation from **`services/commons`**. The aspect wraps the call in an **Observation** named **`commerce.{sanitized_service}.order.detail.cache`** (slug from **`spring.application.name`** — hyphens replaced with underscores), and logs failures at **WARN** with the joinpoint signature. Inside the method, **`MeteredOutcomes.outcome("hit"|"miss")`** tags the active observation with **`result`** and increments the counter with the same base id, exposed as **`commerce_order_service_order_detail_cache_total`** in Prometheus. The annotation never auto-increments — outcomes are explicit, so a branch that should not count simply does not call `outcome(...)`. The global **`application`** tag remains from **`management.metrics.tags`**. Grafana **Order detail cache — hits vs misses** uses these counters (needs **GET `/orders/{id}`**). **`cache_gets_total`** may still exist from Spring Data Redis statistics.

**Compose:** `redis` service (`redis:7-alpine`), persistent volume `redisdata`, host port **6379**. **order-service** waits for Redis healthy before starting.

---

## REST API, producer, and persistence

**REST controller**

```java
package com.example.commerce.order.web;

import com.example.commerce.order.application.PlaceOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

  private final PlaceOrderService placeOrder;

  public OrderController(PlaceOrderService placeOrder) {
    this.placeOrder = placeOrder;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest req) {
    UUID id = placeOrder.handle(req);
    return ResponseEntity.accepted()
        .location(URI.create("/orders/" + id))
        .body(new OrderResponse(id, "PENDING"));
  }

  @GetMapping("/{id}")
  public OrderResponse get(@PathVariable UUID id) {
    return placeOrder.find(id);
  }
}
```

**Producer (publishes `order.created` via `StreamBridge` after persisting)**

```java
package com.example.commerce.order.application;

import com.example.commerce.commons.amqp.EventChannels;
import com.example.commerce.commons.events.OrderCreated;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class PlaceOrderService {

  private final OrderRepository repository;
  private final StreamBridge streamBridge;
  private final Counter placed;

  public PlaceOrderService(OrderRepository repository,
                           StreamBridge streamBridge,
                           MeterRegistry meters) {
    this.repository = repository;
    this.streamBridge = streamBridge;
    this.placed = Counter.builder("orders.placed.count")
        .description("Orders accepted by the order-service")
        .register(meters);
  }

  @Transactional
  public UUID handle(PlaceOrderRequest req) {
    OrderEntity order = OrderEntity.pending(req.customerEmail());
    repository.save(order);

    var event = new OrderCreated(
        UUID.randomUUID(), order.id(), req.customerEmail(),
        req.lines(), Instant.now());

    Message<OrderCreated> message = MessageBuilder.withPayload(event)
        .setHeader(EventChannels.ROUTING_KEY_HEADER, event.routingKey())
        .build();

    streamBridge.send(EventChannels.OUTPUT_BINDING, message);

    placed.increment();
    return order.id();
  }

  public OrderResponse find(UUID id) {
    return repository.findById(id)
        .map(o -> new OrderResponse(o.id(), o.status().name()))
        .orElseThrow();
  }
}
```

> The **OpenTelemetry Java agent** auto-instruments `StreamBridge` and
> the Spring Cloud Stream RabbitMQ binder; W3C `traceparent` headers are
> injected on outgoing messages and extracted on inbound delivery, so
> trace context propagates across HTTP **and** AMQP boundaries with no
> application code.

---
