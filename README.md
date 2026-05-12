# Commerce POC — Traceability & Monitoring

This project is a Proof of Concept (POC) demonstrating a microservices architecture built with **Spring Boot 3.0.9**, featuring a fully functional distributed tracing, metrics, and centralized logging observability stack.

For a deep dive into the architecture, component interactions, observability setup, and the migration path to SolarWinds & Papertrail, please see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

**Flows and edge cases** (happy paths, inventory/payment failures, cache behavior, idempotency, POC payment randomness): [docs/scenarios-and-edge-cases.md](docs/scenarios-and-edge-cases.md).

## Prerequisites

Before starting the project, ensure you have the following installed:
- **Docker** and **Docker Compose**
- **Java 17**
- **Maven 3.8+** (Required to compile the backend services before starting the containers)

## Setup & Startup

1. **Build the microservices:**
   From the root of the project, compile all the Java services using Maven:
   ```bash
   mvn clean package -DskipTests
   ```

2. **Start the complete stack:**
   This will start PostgreSQL, RabbitMQ, the Observability Stack (OTel Collector, Jaeger, Prometheus, Loki, Promtail, Grafana), Eureka, Config Server, API Gateway, all domain services, and the React UI.
   ```bash
   docker compose up -d --build
   ```

3. **Verify the services are healthy:**
   Wait a minute or two for all services to register with Eureka and become healthy. You can check their status using:
   ```bash
   docker compose ps
   ```

## Using the Application

- **React UI**: Available at [http://localhost:5173](http://localhost:5173). You can place a mock order from the Checkout page.
- **API Gateway**: Exposes all backend APIs at [http://localhost:8080](http://localhost:8080).

*Note: The React UI is served automatically via an Nginx container managed by Docker Compose.*

## Exploring Observability

Use this section as a checklist. URLs assume everything runs on `localhost` via Docker Compose.

### Tool URLs and credentials

| What | URL | Notes |
|------|-----|--------|
| React UI | [http://localhost:5173](http://localhost:5173) | Checkout + **Recent orders** table (status polling). |
| API Gateway | [http://localhost:8080](http://localhost:8080) | APIs under `/api/v1/...`. |
| Eureka | [http://localhost:8761](http://localhost:8761) | Service registration (no login). |
| RabbitMQ | [http://localhost:15672](http://localhost:15672) | Default `guest` / `guest` unless overridden in `.env`. |
| Jaeger | [http://localhost:16686](http://localhost:16686) | Distributed traces (no login). |
| Prometheus | [http://localhost:9090](http://localhost:9090) | Metrics scrape UI (no login). |
| Grafana | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` — dashboards + Explore (Loki, Jaeger, Prometheus). |

If you change dashboards, datasources, or Promtail rules under `infra/observability/`, restart so configs reload:

```bash
docker compose restart promtail grafana
```

---

### 1. Generate traffic (do this first)

1. Open the **React UI** and click **Place order** one or more times.
2. Watch **Recent orders**: status goes **PENDING → CONFIRMED** (or **FAILED** if payment declines or inventory fails).
3. Copy an **order id** (UUID) from the table for log searches in §5.

---

### 2. Grafana dashboards

**Dashboards** → browse (provisioned JSON lives under `infra/observability/grafana/dashboards/`).

| Dashboard | What to check |
|-----------|----------------|
| **Commerce — Orders** | Placed / confirmed / failed counters and rates. Set **time range** (e.g. last 6 h). If panels are empty: in Prometheus **Status → Targets**, confirm Spring scrape targets (e.g. `order-service:8080`) are **UP**; then `docker compose restart prometheus grafana`. |
| **Commerce — ERROR logs** | Loki: ERROR-level lines across provisioned services plus per-service volume. |

For **ad-hoc logs** and **traces**, use **Grafana → Explore** (Loki / Jaeger) or the **Jaeger** UI (§3).

---

### 3. Jaeger — traces and spans

**Browse**

1. Open [http://localhost:16686](http://localhost:16686).
2. Choose **Service** (`api-gateway` for UI traffic; `order-service`, `inventory-service`, `payment-service`, `notification-service` as the saga runs).
3. Set **Lookback** → **Find Traces** → open a trace.

**Inspect a span**

- Expand the span → **Tags** (e.g. `commerce.order.id`, `exception.message`, saga outcome fields).
- **Logs** on the span lists **events** (business step annotations).

**Open by trace id**

```text
http://localhost:16686/trace/<paste-trace-id>
```

**Trace → logs**

- From Grafana’s Jaeger datasource, **Related logs** / trace-to-Loki uses the trace id to query Loki.

---

### 4. Grafana Explore — Jaeger

1. **Explore** → data source **Jaeger**.
2. Pick **Service** + time range → run → open a trace (same model as the Jaeger UI).

---

### 5. Grafana Explore — Loki (logs)

**Line shape**

Each log line is **one JSON object** (Logstash composite encoder): `timestamp`, `level`, `logger`, `message`, Micrometer **`traceId`** / **`spanId`** (from MDC), commerce **`orderId`** when set, plus `stack_trace` on errors.

**Labels:** Promtail sets low-cardinality **`service`** (Compose service name) and **`container`**. Trace and span ids stay **inside** the JSON — they are **not** Loki labels.

**Examples**

Gateway only:

```logql
{service="api-gateway"}
```

Parse JSON and filter by trace (id from Jaeger or **X-Trace-Id**):

```logql
{service=~".+"} | json | traceId="PASTE_TRACE_ID"
```

One span id:

```logql
{service=~".+"} | json | spanId="PASTE_SPAN_ID"
```

Follow one order everywhere (`orderId` is inside `mdc` in JSON):

```logql
{service=~".+"} | json | orderId="PASTE_FULL_ORDER_UUID"
```

Combine:

```logql
{service="payment-service"} | json | traceId="PASTE_TRACE_ID"
```

**Row actions**

- **TraceID** derived field → open Jaeger.
- **OrderId** / **SpanId** derived fields → highlight values for copy-paste into queries above.

---

### 6. API Gateway response headers (trace ids in the browser)

Successful gateway responses expose **`traceparent`**, **`X-Trace-Id`**, **`X-Span-Id`** (CORS **Expose-Headers** for the dev UI). DevTools → Network → select a request → **Response headers** → copy **X-Trace-Id** for Jaeger or Loki.

---

### 7. Prometheus — quick check

1. [http://localhost:9090](http://localhost:9090) → **Status → Targets** (scrapes should be **UP**).
2. **Graph**: try e.g. `orders_placed_count_total`.

---

### 8. RabbitMQ and Eureka (optional)

- **RabbitMQ**: queues / rates for the topic exchange used by the saga.
- **Eureka**: all app instances **UP** before blaming the gateway for `503` / empty load balancer.

---

### 9. More documentation

| Doc | Purpose |
|-----|---------|
| **[docs/runbook.md](docs/runbook.md)** | Startup, log correlation notes, teardown, common failures. |
| **[docs/observability-primer.md](docs/observability-primer.md)** | Plain-language intro to OpenTelemetry, Grafana, Loki, Promtail + sample queries. |
| **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** | Deep design and backend migration (e.g. SolarWinds). |
| **[docs/architecture/](docs/architecture/README.md)** | Per-service architecture notes (order, inventory, payment, notification). |

## Troubleshooting

### RabbitMQ: `ACCESS_REFUSED` / invalid credentials for user `guest`

Two things commonly cause this:

1. **Stale Docker volume** — RabbitMQ persists users in the named volume. If credentials changed in `.env`, reset volumes:
   ```bash
   docker compose down -v
   docker compose up -d --build
   ```

2. **Guest from other containers** — The broker config enables remote `guest` (`loopback_users.guest = false`). Ensure `RABBITMQ_USER` and `RABBITMQ_PASSWORD` in `.env` match what you expect.

### PostgreSQL: `password authentication failed for user "${POSTGRES_USER}"` or Hibernate dialect errors

Config-repo JDBC URLs use `${POSTGRES_USER}` / `${POSTGRES_PASSWORD}`. Those tokens are resolved from each **service container’s** environment, which Compose fills from your **`.env`** (same values as the `postgres` service). Define `POSTGRES_USER` and `POSTGRES_PASSWORD` in `.env`, then rebuild or recreate:

```bash
docker compose up -d --build
```

If you changed DB credentials after the first run, reset the DB volume (this wipes data):

```bash
docker compose down -v
docker compose up -d --build
```

### PostgreSQL: I don’t see any tables (or empty databases)

This stack uses **three logical databases** (`orders`, `inventory`, `payments`). They are created by **`infra/postgres/init.sql`** the **first time** the Postgres data volume is initialized — not on every boot.

**Important:**

1. **`init.sql` does not create tables.** Tables such as **`orders`** are created by **Hibernate** (`ddl-auto: update` from Config Server) when **`order-service`** (and the other services) start and connect successfully.
2. From your host, connect on mapped port **`5438`**, not `5432`:
   - Example JDBC / URL: `jdbc:postgresql://localhost:5438/orders` with user/password from `.env`.
3. In a SQL GUI, select database **`orders`** (not the default `postgres` DB) to see the order-service tables.

Verify from the shell (adjust user/password if needed):

```bash
docker compose exec postgres psql -U postgres -d orders -c '\dt'
```

If `\dt` is empty but services are healthy, check `docker compose logs order-service` for datasource/JPA errors. If the **`orders`** database itself is missing, your volume was probably created before `init.sql` existed — reset and recreate:

```bash
docker compose down -v
docker compose up -d --build
```

### API Gateway: `No servers available for service: order-service` (and 503)

The gateway resolves `lb://order-service` via Eureka. That warning appears when a request hits the gateway **before** `order-service` has registered (or after it has stopped). The stack waits for JVM health on `order-service`, `inventory-service`, and `payment-service` before starting the gateway, and the gateway applies a **Retry** filter on 503 for early traffic.

If it persists, confirm **ORDER-SERVICE** is **UP** on [http://localhost:8761](http://localhost:8761) and check `docker compose logs order-service`.

## Teardown

To stop all services and remove the network:
```bash
docker compose down
```

To stop all services and **wipe all data volumes** (databases, rabbitmq data, loki data, etc.):
```bash
docker compose down -v
```

## Next Steps & Migration

This POC is configured out-of-the-box to use a 100% local, OSS observability stack (Jaeger, Prometheus, Loki) via an OpenTelemetry Collector. 

If you want to migrate telemetry data to **SolarWinds Observability** and logs to **Papertrail**, follow the exact migration steps outlined in **[Section 22 of ARCHITECTURE.md](docs/ARCHITECTURE.md#22-migration-path--swap-oss-backends-for-solarwinds-observability-and-papertrail)**.
