package com.example.commerce.notification.application;

import com.example.commerce.commons.events.InventoryFailed;
import com.example.commerce.commons.events.PaymentCompleted;
import com.example.commerce.commons.events.PaymentFailed;
import com.example.commerce.commons.logging.OrderIdMdc;
import com.example.commerce.commons.metrics.CommerceMetered;
import com.example.commerce.commons.metrics.MeteredOutcomes;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationDispatchConsumer {

  private static final Logger monitoring = LoggerFactory.getLogger("monitoring");

  private final Counter paymentCompletedCounter;
  private final Counter paymentFailedCounter;
  private final Counter inventoryFailedCounter;

  public NotificationDispatchConsumer(MeterRegistry meterRegistry) {
    this.paymentCompletedCounter =
        Counter.builder("notifications.sent.count")
            .tag("type", "payment_completed")
            .description("Number of payment completed notifications sent")
            .register(meterRegistry);
    this.paymentFailedCounter =
        Counter.builder("notifications.sent.count")
            .tag("type", "payment_failed")
            .description("Number of payment failed notifications sent")
            .register(meterRegistry);
    this.inventoryFailedCounter =
        Counter.builder("notifications.sent.count")
            .tag("type", "inventory_failed")
            .description("Number of inventory failed notifications sent")
            .register(meterRegistry);
  }

  @CommerceMetered("notification.payment.completed")
  public void onPaymentCompleted(PaymentCompleted e) {
    OrderIdMdc.run(
        e.orderId(),
        () -> {
          MeteredOutcomes.lowCardinalityTag("commerce.order.id", e.orderId().toString());
          paymentCompletedCounter.increment();
          MeteredOutcomes.outcome("sent");
          monitoring.info(
              "action=NOTIFICATION_SENT type=PAYMENT_COMPLETED orderId={} transactionId={}",
              e.orderId(),
              e.transactionId());
        });
  }

  @CommerceMetered("notification.payment.failed")
  public void onPaymentFailed(PaymentFailed e) {
    OrderIdMdc.run(
        e.orderId(),
        () -> {
          MeteredOutcomes.lowCardinalityTag("commerce.order.id", e.orderId().toString());
          paymentFailedCounter.increment();
          MeteredOutcomes.outcome("sent");
          monitoring.warn(
              "action=NOTIFICATION_SENT type=PAYMENT_FAILED orderId={} reason={}",
              e.orderId(),
              e.reason());
        });
  }

  @CommerceMetered("notification.inventory.failed")
  public void onInventoryFailed(InventoryFailed e) {
    OrderIdMdc.run(
        e.orderId(),
        () -> {
          MeteredOutcomes.lowCardinalityTag("commerce.order.id", e.orderId().toString());
          inventoryFailedCounter.increment();
          MeteredOutcomes.outcome("sent");
          monitoring.warn(
              "action=NOTIFICATION_SENT type=INVENTORY_FAILED orderId={} reason={}",
              e.orderId(),
              e.reason());
        });
  }
}
