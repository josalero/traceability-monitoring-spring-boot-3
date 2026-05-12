package com.example.commerce.commons.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailed(
    UUID eventId, UUID orderId, String reason, Instant occurredAt) implements DomainEvent {
  @Override
  public String routingKey() { return "payment.failed"; }
}
