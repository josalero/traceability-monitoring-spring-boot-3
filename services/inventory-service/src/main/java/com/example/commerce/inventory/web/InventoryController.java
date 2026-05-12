package com.example.commerce.inventory.web;

import com.example.commerce.inventory.persistence.StockEntity;
import com.example.commerce.inventory.persistence.StockRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
  
  private final StockRepository stockRepository;

  public InventoryController(StockRepository stockRepository) {
    this.stockRepository = stockRepository;
  }

  @GetMapping("/{sku}")
  public ResponseEntity<StockEntity> getStock(@PathVariable String sku) {
    return stockRepository.findById(sku)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
