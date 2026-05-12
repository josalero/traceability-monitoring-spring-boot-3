package com.example.commerce.order.application;

import com.example.commerce.commons.events.InventoryFailed;
import com.example.commerce.commons.events.PaymentCompleted;
import com.example.commerce.commons.events.PaymentFailed;
import com.example.commerce.commons.logging.OrderIdMdc;
import com.example.commerce.commons.metrics.CommerceMetered;
import com.example.commerce.commons.metrics.MeteredOutcomes;
import com.example.commerce.order.persistence.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderStatusConsumer {

  private static final Logger monitoring = LoggerFactory.getLogger("monitoring");

  private final OrderRepository repository;
  private final OrderCacheEvictor cacheEvictor;
  private final Counter confirmedCounter;
  private final Counter failedPaymentCounter;
  private final Counter failedInventoryCounter;

  public OrderStatusConsumer(
      OrderRepository repository,
      OrderCacheEvictor cacheEvictor,
      MeterRegistry meterRegistry) {
    this.repository = repository;
    this.cacheEvictor = cacheEvictor;
    this.confirmedCounter =
        Counter.builder("orders.confirmed.count")
            .description("Orders successfully confirmed")
            .register(meterRegistry);
    this.failedPaymentCounter =
        Counter.builder("orders.failed.count")
            .tag("reason", "payment_failed")
            .description("Orders failed due to payment issues")
            .register(meterRegistry);
    this.failedInventoryCounter =
        Counter.builder("orders.failed.count")
            .tag("reason", "inventory_failed")
            .description("Orders failed due to out of stock")
            .register(meterRegistry);
  }

  @CommerceMetered("on.payment.completed")
  public void onPaymentCompleted(PaymentCompleted e) {
    OrderIdMdc.run(
        e.orderId(),
        () -> {
          MeteredOutcomes.lowCardinalityTag("commerce.order.id", e.orderId().toString());
          repository
              .findById(e.orderId())
              .ifPresentOrElse(
                  order -> {
                    order.confirm();
                    repository.save(order);
                    cacheEvictor.evict(order.id());
                    confirmedCounter.increment();
                    MeteredOutcomes.event("order.confirmed");
                    MeteredOutcomes.outcome("confirmed");
                    monitoring.info(
                        "action=ORDER_CONFIRMED orderId={} transactionId={}",
                        e.orderId(),
                        e.transactionId());
                  },
                  () ->
                      monitoring.warn(
                          "action=ORDER_CONFIRMED_SKIPPED orderId={} reason=not_found",
                          e.orderId()));
        });
  }

  @CommerceMetered(
      value = "on.payment.failed",
      tags = {"commerce.saga.stage", "PAYMENT"})
  public void onPaymentFailed(PaymentFailed e) {
    OrderIdMdc.run(
        e.orderId(),
        () -> {
          MeteredOutcomes.lowCardinalityTag("commerce.order.id", e.orderId().toString());
          repository
              .findById(e.orderId())
              .ifPresentOrElse(
                  order -> {
                    order.fail();
                    repository.save(order);
                    cacheEvictor.evict(order.id());
                    failedPaymentCounter.increment();
                    MeteredOutcomes.highCardinalityTag(
                        "commerce.saga.reason", truncate(e.reason(), 512));
                    MeteredOutcomes.error(
                        new RuntimeException(
                            "saga_failed_payment: " + truncate(e.reason(), 256)));
                    MeteredOutcomes.outcome("failed");
                    monitoring.warn(
                        "action=ORDER_FAILED stage=PAYMENT orderId={} reason={}",
                        e.orderId(),
                        e.reason());
                  },
                  () ->
                      monitoring.warn(
                          "action=ORDER_FAILED_SKIPPED stage=PAYMENT orderId={} reason=not_found",
                          e.orderId()));
        });
  }

  @CommerceMetered(
      value = "on.inventory.failed",
      tags = {"commerce.saga.stage", "INVENTORY"})
  public void onInventoryFailed(InventoryFailed e) {
    OrderIdMdc.run(
        e.orderId(),
        () -> {
          MeteredOutcomes.lowCardinalityTag("commerce.order.id", e.orderId().toString());
          repository
              .findById(e.orderId())
              .ifPresentOrElse(
                  order -> {
                    order.fail();
                    repository.save(order);
                    cacheEvictor.evict(order.id());
                    failedInventoryCounter.increment();
                    MeteredOutcomes.highCardinalityTag(
                        "commerce.saga.reason", truncate(e.reason(), 512));
                    MeteredOutcomes.error(
                        new RuntimeException(
                            "saga_failed_inventory: " + truncate(e.reason(), 256)));
                    MeteredOutcomes.outcome("failed");
                    monitoring.warn(
                        "action=ORDER_FAILED stage=INVENTORY orderId={} reason={}",
                        e.orderId(),
                        e.reason());
                  },
                  () ->
                      monitoring.warn(
                          "action=ORDER_FAILED_SKIPPED stage=INVENTORY orderId={} reason=not_found",
                          e.orderId()));
        });
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max) + "…";
  }
}
