package com.example.commerce.commons.events;

import java.time.Instant;
import java.util.UUID;

public record OrderCreated(
    UUID eventId, UUID orderId, String customerEmail,
    java.util.List<OrderLine> lines, Instant occurredAt) implements DomainEvent {
  @Override
  public String routingKey() { return "order.created"; }
}
