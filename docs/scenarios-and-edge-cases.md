# Commerce POC — Happy paths, failures, and corner cases

This document describes **expected outcomes** (including deliberate POC shortcuts), **failure branches**, and **edge cases** that show up in logs, metrics, or traces. Class and package names refer to `services/*`; where sketches in [ARCHITECTURE.md](ARCHITECTURE.md) differ, **code wins**.

---

## 1. Simulated “random” payment declines (POC only)

The payment simulator intentionally declines **~10%** of charges:

From `services/payment-service/.../PaymentGateway.java`:

```java
public String charge(UUID orderId) {
  // Simulated payment logic
  if (Math.random() < 0.1) {
    throw new PaymentDeclinedException("Card declined for order " + orderId);
  }
  return "txn_" + UUID.randomUUID().toString().substring(0, 8);
}
```

That is **not** mysterious infrastructure noise: replace `PaymentGateway` with a real PSP integration for deterministic production behavior. Until then, placing many orders will produce some **`PAYMENT_DECLINED`** / **`payment.failed`** paths **by design**.

---

## 2. Happy path (checkout saga)

Typical sequence when inventory and payment both succeed:

| Step | Component | What happens |
|------|-----------|----------------|
| 1 | **API** → `order-service` | `POST /orders` persists **`PENDING`**, publishes **`order.created`**. |
| 2 | **inventory-service** | `InventoryReservationWorkflow.completeReservation` reserves stock, marks event processed, publishes **`inventory.reserved`**. |
| 3 | **payment-service** | `ChargePaymentConsumer` charges via `PaymentGateway`, publishes **`payment.completed`**, marks processed. |
| 4 | **order-service** | `OrderStatusConsumer.onPaymentCompleted` sets **`CONFIRMED`**, evicts Redis cache key for that order. |
| 5 | **notification-service** | Logs / counts “notification sent” for **`payment.completed`** (mock notifier). |

**UI:** **`GET /orders/{id}`** polls until status leaves **`PENDING`**; **`CONFIRMED`** / **`FAILED`** responses may be served from Redis once cached.

---

## 3. Failure paths (still “successful” from a messaging perspective)

### 3.1 Inventory cannot reserve

**Triggers**

- Not enough quantity on a SKU (`StockEntity.reserve`).
- SKU row missing (`OutOfStockException` with “SKU not found: …”).

**Behavior**

- `StockService.reserveAll` throws **`OutOfStockException`**. It is declared **`noRollbackFor`** so the workflow transaction can commit after handling failure.
- `InventoryReservationWorkflow` catches it, records **`failed`** outcome, **`markProcessed`**, returns **`InventoryFailed`** (published downstream).
- **order-service** `OrderStatusConsumer.onInventoryFailed` sets order **`FAILED`** (tag **`inventory_failed`**), evicts cache.

**Happy-path termination:** saga stops before payment; no **`inventory.reserved`** for that attempt.

### 3.2 Payment declines

**Triggers**

- **`PaymentDeclinedException`** from **`PaymentGateway`** (including the random simulator).

**Behavior**

- **`ChargePaymentConsumer`** publishes **`payment.failed`**, increments failure metrics, **`markProcessed`** on the **`inventory.reserved`** event id.
- **order-service** `onPaymentFailed` sets **`FAILED`** (payment reason), evicts cache.

### 3.3 Duplicate events (idempotency)

| Event | Guard | Outcome |
|-------|--------|---------|
| **`OrderCreated`** | `ProcessedEventRepository.existsByEventId` in workflow | Empty optional → consumer records **`skipped`**; no second reservation. |
| **`InventoryReserved`** | `ChargePaymentConsumer` processed table | **`skipped` / duplicate_event**; no second charge. |

Binder **retries** and **DLQ** (see service YAML) can redeliver messages; idempotency prevents double side-effects.

---

## 4. HTTP API — order-service

### 4.1 Happy path

- **`POST /orders`** — **202 Accepted** with **`PENDING`** and **`Location: /orders/{id}`** when validation passes.
- **`GET /orders/{id}`** — **200** with current status.

### 4.2 Failures

| Situation | HTTP | Notes |
|-----------|------|--------|
| Bean validation on **`PlaceOrderRequest`** | **400** | `OrderWebExceptionHandler` maps **`MethodArgumentNotValidException`** to Problem Detail; HTTP observation tagged **`validation_failed`**. |
| Unknown order id | **404** | `PlaceOrderService.find` → **`NoSuchElementException`** → **`order_not_found`**. `@CommerceMetered(..., logFailures = false)` avoids treating this as an aspect “method failure” WARN. |

### 4.3 Order detail cache (Redis) — corners

Implemented in **`OrderReadService.loadOrderDetail`**:

| Case | Behavior |
|------|-----------|
| Cache **hit** | Return cached **`OrderResponse`**; metric **`hit`**. |
| Cache **miss** | Load from Postgres; metric **`miss`**. |
| Status **`PENDING`** | **Never** written to cache — avoids stale polling during the saga. |
| **`CONFIRMED`** / **`FAILED`** | Stored in cache (TTL from config) after DB read. |
| Row updated after confirm/fail | **`OrderCacheEvictor`** evicts so the next read refreshes. |
| Corrupt cache entry (wrong Java type) | **`IllegalStateException`** — treated as a real defect; **`@CommerceMetered("order.detail.cache")`** still logs failures at WARN. |

---

## 5. Order row missing when a terminal event arrives

**payment.completed**, **payment.failed**, and **inventory.failed** handlers use **`findById(...).ifPresentOrElse(...)`**.

If the row is gone (manual DB edit, env mismatch, or replay of very old events):

- Consumer logs a **skipped** line (**`ORDER_CONFIRMED_SKIPPED`** / **`ORDER_FAILED_SKIPPED`** with **`reason=not_found`**).
- No counter increment on the “happy” branch; observability still shows the handler ran.

---

## 6. Observability noise vs real defects

- **Expected domain outcomes** that still throw or complete with “errors” on observations may use **`@CommerceMetered(logFailures = false)`** so **`CommerceMeteredAspect`** does not emit **`metered_method_failed`** WARN lines. Examples: **`PlaceOrderService.find`** (404), **`StockService.reserveAll`** ( **`OutOfStockException`** still recorded on the observation; caller maps to **`InventoryFailed`**).
- **RPC validation**, **cache corruption**, and **unexpected exceptions** keep default **`logFailures = true`** so WARN logs remain useful.

For stack wiring (OTel, Loki, Grafana), see [observability-primer.md](observability-primer.md).

---

## 7. Quick mental model

```text
POST /orders ──► PENDING ──► order.created ──► inventory ──┬──► inventory.reserved ──► payment ──┬──► payment.completed ──► CONFIRMED
                                                           └──► inventory.failed ───► FAILED (inventory)
                                                                                             └──► payment.failed ───► FAILED (payment)
```

Only one of **`inventory.failed`** or **`inventory.reserved`** should publish per **`OrderCreated`** event id; payment runs only on **`inventory.reserved`**.
