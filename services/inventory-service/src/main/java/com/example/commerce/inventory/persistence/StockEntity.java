package com.example.commerce.inventory.persistence;

import com.example.commerce.inventory.domain.OutOfStockException;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock")
public class StockEntity {
  @Id
  private String sku;
  private int quantity;

  protected StockEntity() {}

  public StockEntity(String sku, int quantity) {
    this.sku = sku;
    this.quantity = quantity;
  }

  public String getSku() { return sku; }
  public int getQuantity() { return quantity; }

  public void reserve(int amount) {
    if (this.quantity < amount) {
      throw new OutOfStockException("Not enough stock for " + sku);
    }
    this.quantity -= amount;
  }
}
