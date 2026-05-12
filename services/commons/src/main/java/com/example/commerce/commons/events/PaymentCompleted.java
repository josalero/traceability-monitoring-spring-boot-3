package com.example.commerce.commons.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompleted(
    UUID eventId, UUID orderId, String transactionId, Instant occurredAt) implements DomainEvent {
  @Override
  public String routingKey() { return "payment.completed"; }
}
