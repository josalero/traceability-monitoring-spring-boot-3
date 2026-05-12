package com.example.commerce.order.web;

import com.example.commerce.order.application.PlaceOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

/**
 * Paths use {@code /orders} so they match the API Gateway {@code RewritePath} to {@code order-service}.
 */
@Validated
@RestController
@RequestMapping("/orders")
public class OrderController {

  private final PlaceOrderService placeOrder;

  public OrderController(PlaceOrderService placeOrder) {
    this.placeOrder = placeOrder;
  }

  @GetMapping
  public ResponseEntity<List<OrderSummaryResponse>> list(
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore().mustRevalidate())
        .body(placeOrder.listRecent(limit));
  }

  @PostMapping
  public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest req) {
    UUID id = placeOrder.handle(req);
    return ResponseEntity.accepted()
        .location(URI.create("/api/v1/orders/" + id))
        .body(new OrderResponse(id, "PENDING"));
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore().mustRevalidate())
        .body(placeOrder.find(id));
  }
}
