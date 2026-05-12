package com.example.commerce.payment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
  default boolean existsByEventId(UUID eventId) {
    return existsById(eventId);
  }
  
  default void markProcessed(UUID eventId) {
    save(new ProcessedEventEntity(eventId));
  }
}
