# Payment service

> **payment-service** — consumes `inventory.reserved`, charges via `PaymentGateway`, emits payment outcome events.
>
> [← Back to architecture overview](../ARCHITECTURE.md)

## 14. Payment service — Stream Consumer sketch

```java
@Bean
public Consumer<Message<InventoryReserved>> chargePayment(
    PaymentGateway gateway,
    ProcessedEventRepository processed,
    StreamBridge streamBridge) {

  return message -> {
    InventoryReserved event = message.getPayload();
    if (processed.existsByEventId(event.eventId())) return;

    DomainEvent result;
    try {
      String txId = gateway.charge(event.orderId());
      result = new PaymentCompleted(UUID.randomUUID(), event.orderId(), txId, Instant.now());
    } catch (PaymentDeclinedException ex) {
      result = new PaymentFailed(UUID.randomUUID(), event.orderId(),
          ex.getMessage(), Instant.now());
    }

    streamBridge.send(EventChannels.OUTPUT_BINDING,
        MessageBuilder.withPayload(result)
            .setHeader(EventChannels.ROUTING_KEY_HEADER, result.routingKey())
            .build());

    processed.markProcessed(event.eventId());
  };
}
```

`payment-service` binding (excerpt):

```yaml
spring:
  cloud:
    function:
      definition: chargePayment
    stream:
      bindings:
        chargePayment-in-0:
          destination: commerce.events
          group: payment-service
      rabbit:
        bindings:
          chargePayment-in-0:
            consumer:
              exchangeType: topic
              bindingRoutingKey: inventory.reserved
              autoBindDlq: true
              republishToDlq: true
```

---

