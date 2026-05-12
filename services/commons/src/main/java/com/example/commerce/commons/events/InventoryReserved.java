package com.example.commerce.commons.events;

import java.time.Instant;
import java.util.UUID;

public record InventoryReserved(
    UUID eventId, UUID orderId, Instant occurredAt) implements DomainEvent {
  @Override
  public String routingKey() { return "inventory.reserved"; }
}
