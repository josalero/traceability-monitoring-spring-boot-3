package com.example.commerce.commons.events;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreated.class, name = "OrderCreated"),
    @JsonSubTypes.Type(value = InventoryReserved.class, name = "InventoryReserved"),
    @JsonSubTypes.Type(value = InventoryFailed.class, name = "InventoryFailed"),
    @JsonSubTypes.Type(value = PaymentCompleted.class, name = "PaymentCompleted"),
    @JsonSubTypes.Type(value = PaymentFailed.class, name = "PaymentFailed")
})
public sealed interface DomainEvent
    permits OrderCreated, InventoryReserved, InventoryFailed,
            PaymentCompleted, PaymentFailed {

  UUID eventId();
  UUID orderId();
  Instant occurredAt();
  String routingKey();
}
