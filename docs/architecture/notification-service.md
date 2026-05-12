# Notification service

> **notification-service** — terminal typed consumers for payment/inventory outcomes (logging / notification sketch).
>
> [← Back to architecture overview](../ARCHITECTURE.md)

## 15. Notification service — terminal Stream Consumers

Stream binder consumers are typed. Instead of one polymorphic listener
plus `instanceof`, define **one `Consumer<T>` bean per event type** and
bind each to its own routing key. All share the same destination queue
group, so a single deployment unit handles all terminal events but with
type-safe payloads.

```java
@Bean
public Consumer<PaymentCompleted> onPaymentCompleted() {
  return e -> log.info("email: order {} confirmed (tx={})", e.orderId(), e.transactionId());
}

@Bean
public Consumer<PaymentFailed> onPaymentFailed() {
  return e -> log.warn("email: order {} payment failed: {}", e.orderId(), e.reason());
}

@Bean
public Consumer<InventoryFailed> onInventoryFailed() {
  return e -> log.warn("email: order {} out of stock: {}", e.orderId(), e.reason());
}
```

`notification-service` `application.yml`:

```yaml
spring:
  cloud:
    function:
      definition: onPaymentCompleted;onPaymentFailed;onInventoryFailed
    stream:
      bindings:
        onPaymentCompleted-in-0:
          destination: commerce.events
          group: notification-service-payment-completed
        onPaymentFailed-in-0:
          destination: commerce.events
          group: notification-service-payment-failed
        onInventoryFailed-in-0:
          destination: commerce.events
          group: notification-service-inventory-failed
      rabbit:
        bindings:
          onPaymentCompleted-in-0:
            consumer:
              exchangeType: topic
              bindingRoutingKey: payment.completed
              autoBindDlq: true
          onPaymentFailed-in-0:
            consumer:
              exchangeType: topic
              bindingRoutingKey: payment.failed
              autoBindDlq: true
          onInventoryFailed-in-0:
            consumer:
              exchangeType: topic
              bindingRoutingKey: inventory.failed
              autoBindDlq: true
```

> The **order-service** uses the same three-bean pattern (with `group:
> order-service`) to consume terminal events and update its persisted
> order to `CONFIRMED` or `FAILED`.

---

