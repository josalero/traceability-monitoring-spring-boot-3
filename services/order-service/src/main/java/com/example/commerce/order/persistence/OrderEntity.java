package com.example.commerce.order.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "orders")
public class OrderEntity {

  @Id
  private UUID id;

  private String customerEmail;

  @Enumerated(EnumType.STRING)
  private Status status;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  protected OrderEntity() {}

  public static OrderEntity pending(String customerEmail) {
    OrderEntity o = new OrderEntity();
    o.id = UUID.randomUUID();
    o.customerEmail = customerEmail;
    o.status = Status.PENDING;
    return o;
  }

  public UUID id() { return id; }
  public Status status() { return status; }
  public Instant createdAt() { return createdAt; }
  public void confirm() { this.status = Status.CONFIRMED; }
  public void fail() { this.status = Status.FAILED; }

  public enum Status { PENDING, CONFIRMED, FAILED }
}
