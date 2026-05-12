package com.example.commerce.payment.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {
  @Id
  private UUID eventId;

  protected ProcessedEventEntity() {}

  public ProcessedEventEntity(UUID eventId) {
    this.eventId = eventId;
  }
}
