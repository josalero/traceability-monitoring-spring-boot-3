package com.example.commerce.inventory.application;

import com.example.commerce.commons.events.OrderLine;
import com.example.commerce.commons.metrics.CommerceMetered;
import com.example.commerce.commons.metrics.MeteredOutcomes;
import com.example.commerce.inventory.domain.OutOfStockException;
import com.example.commerce.inventory.persistence.StockEntity;
import com.example.commerce.inventory.persistence.StockRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

  private final StockRepository stockRepository;

  public StockService(StockRepository stockRepository) {
    this.stockRepository = stockRepository;
  }

  /**
   * Deducts stock under pessimistic locks. Instrumented with {@link CommerceMetered} for latency and
   * tags ({@code commerce.line.count}, {@code commerce.order.id}); legacy reservation counters stay on
   * {@link com.example.commerce.inventory.application.ReserveOrderCreatedConsumer}.
   *
   * <p>{@link OutOfStockException} is a normal business outcome when quantity cannot be reserved.
   * It must not mark the surrounding transaction rollback-only ({@code noRollbackFor}) so {@link
   * InventoryReservationWorkflow} can persist idempotency and emit {@code InventoryFailed}. Expected
   * failures are not logged by the metered aspect ({@code logFailures = false}).
   */
  @CommerceMetered(value = "inventory.reserve", logFailures = false)
  @Transactional(noRollbackFor = OutOfStockException.class)
  public void reserveAll(UUID orderId, List<OrderLine> lines) {
    MeteredOutcomes.lowCardinalityTag("commerce.line.count", String.valueOf(lines.size()));
    MeteredOutcomes.highCardinalityTag("commerce.order.id", orderId.toString());
    for (OrderLine line : lines) {
      StockEntity stock =
          stockRepository
              .findBySkuForUpdate(line.sku())
              .orElseThrow(() -> new OutOfStockException("SKU not found: " + line.sku()));
      stock.reserve(line.quantity());
      stockRepository.save(stock);
    }
  }
}
