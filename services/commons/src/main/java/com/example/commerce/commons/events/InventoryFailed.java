package com.example.commerce.commons.events;

import java.time.Instant;
import java.util.UUID;

public record InventoryFailed(
    UUID eventId, UUID orderId, String reason, Instant occurredAt) implements DomainEvent {
  @Override
  public String routingKey() { return "inventory.failed"; }
}
