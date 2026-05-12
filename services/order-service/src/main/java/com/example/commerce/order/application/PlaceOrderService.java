package com.example.commerce.order.application;

import com.example.commerce.commons.amqp.EventChannels;
import com.example.commerce.commons.events.OrderCreated;
import com.example.commerce.commons.metrics.CommerceMetered;
import com.example.commerce.commons.metrics.MeteredOutcomes;
import com.example.commerce.commons.logging.OrderIdMdc;
import com.example.commerce.order.persistence.OrderEntity;
import com.example.commerce.order.persistence.OrderRepository;
import com.example.commerce.order.web.OrderResponse;
import com.example.commerce.order.web.OrderSummaryResponse;
import com.example.commerce.order.web.PlaceOrderRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceOrderService {

  private static final Logger monitoring = LoggerFactory.getLogger("monitoring");

  private final OrderRepository repository;
  private final OrderReadService orderReadService;
  private final StreamBridge streamBridge;
  private final Counter placed;

  public PlaceOrderService(
      OrderRepository repository,
      OrderReadService orderReadService,
      StreamBridge streamBridge,
      MeterRegistry meters) {
    this.repository = repository;
    this.orderReadService = orderReadService;
    this.streamBridge = streamBridge;
    this.placed =
        Counter.builder("orders.placed.count")
            .description("Orders accepted by the order-service")
            .register(meters);
  }

  /**
   * Accepts an order and publishes {@link OrderCreated}. {@link CommerceMetered} adds observation +
   * outcome counter {@code commerce.order_service.order.place} with {@code result=accepted} alongside
   * legacy {@code orders.placed.count}.
   */
  @CommerceMetered("order.place")
  @Transactional
  public UUID handle(PlaceOrderRequest req) {
    MeteredOutcomes.lowCardinalityTag("commerce.line.count", String.valueOf(req.lines().size()));

    OrderEntity order = OrderEntity.pending(req.customerEmail());
    repository.save(order);
    UUID oid = order.id();

    MeteredOutcomes.highCardinalityTag("commerce.order.id", oid.toString());

    OrderIdMdc.run(
        oid,
        () -> {
          var event =
              new OrderCreated(
                  UUID.randomUUID(), oid, req.customerEmail(), req.lines(), Instant.now());
          streamBridge.send(
              EventChannels.OUTPUT_BINDING,
              MessageBuilder.withPayload(event)
                  .setHeader(EventChannels.ROUTING_KEY_HEADER, event.routingKey())
                  .build());

          MeteredOutcomes.event("order.created.published");
          placed.increment();

          monitoring.info("action=ORDER_ACCEPTED orderId={} lineItems={}", oid, req.lines().size());
        });

    MeteredOutcomes.outcome("accepted");
    return oid;
  }

  /**
   * Resolves {@code GET /orders/{id}} via cache-aside {@link OrderReadService}. Missing orders yield
   * HTTP 404 from the controller exception handler. {@link CommerceMetered} publishes counters
   * {@code commerce.order_service.order.lookup} with {@code result=found|not_found}; the metered aspect
   * does not log those failures ({@code logFailures = false}) because missing orders are expected.
   */
  @CommerceMetered(value = "order.lookup", logFailures = false)
  public OrderResponse find(UUID id) {
    OrderResponse cachedOrLoaded = orderReadService.loadOrderDetail(id);
    if (cachedOrLoaded == null) {
      MeteredOutcomes.outcome("not_found");
      throw new NoSuchElementException();
    }
    MeteredOutcomes.outcome("found");
    return cachedOrLoaded;
  }

  /** Recent orders for dashboards (newest first). {@code limit} is capped at 100. */
  public List<OrderSummaryResponse> listRecent(int limit) {
    int capped = Math.min(Math.max(limit, 1), 100);
    var page = PageRequest.of(0, capped, Sort.by(Sort.Direction.DESC, "createdAt"));
    return repository.findAll(page).stream()
        .map(o -> new OrderSummaryResponse(o.id(), o.status().name(), o.createdAt()))
        .toList();
  }
}
