# Commerce POC Runbook

## 1. Prerequisites
- Docker and Docker Compose
- Java 17
- Maven 3.8+ (or just use `docker compose build`)
- Node.js 18+ (for running the React UI locally, optional if you rely on a built container, but our compose setup assumes `npm run dev`)

## 2. Startup
Run the following from the root directory (`commerce-poc`):

```bash
docker compose up -d
```

Ensure all services start successfully. You can monitor the health of the services using:

```bash
docker compose ps
```

The gateway exposes all APIs at `http://localhost:8080`.

## 3. React UI
Start the frontend:

```bash
cd web-ui
npm install
npm run dev
```

Visit `http://localhost:5173`. You can place an order using the Checkout page.

## 4. Observability Stack

If these tools are new to you, read **[observability-primer.md](observability-primer.md)** first — it explains OpenTelemetry, Grafana, Loki, and Promtail in plain language with sample queries.

Once you have placed an order, explore the observability tools:

- **RabbitMQ Management**: `http://localhost:15672` (guest/guest)
- **Eureka Service Registry**: `http://localhost:8761`
- **Jaeger (Traces)**: `http://localhost:16686`
- **Prometheus (Metrics)**: `http://localhost:9090`
- **Grafana (Dashboards & Logs)**: `http://localhost:3000` (admin/admin)

### Dynatrace POC (Spring Boot 3.0.9 fixed)

To validate **OneAgent Java traces (PurePath)**, **host/process monitoring**, and **Micrometer metrics** in **Dynatrace SaaS** without starting the local Jaeger / Grafana / Prometheus / Loki stack, follow **[DYNATRACE-POC.md](DYNATRACE-POC.md)** and run **`docker compose -f docker-compose.dynatrace-poc.yml up -d --build`** (**Linux**). That path keeps **`spring-boot-starter-parent` 3.0.9**, runs **`dynatrace/oneagent`**, **does not** use the OpenTelemetry javaagent, and uses **`config-repo/application-dynatrace.yml`** for metrics (`SPRING_PROFILES_ACTIVE=cloud,dynatrace`). It is separate from the default `docker compose up` observability flow above (add **`--profile commerce-local-observability`** to the Dynatrace compose command if you also want the local OSS stack).

### Logs (Loki) — trace id, span id, order id

Log lines are **JSON**. Micrometer tracing puts **`traceId`** and **`spanId`** into SLF4J MDC; the Logstash encoder emits them as **top-level** JSON fields alongside `message`, `logger`, and `level`. Commerce sets **`orderId`** in MDC for saga-related handlers once an order id is known.

**Promtail** adds only **`service`** and **`container`** labels. Trace and span ids are **not** labels — query them with **`| json`** (or line filters like Jaeger’s trace-to-logs link).

Example LogQL in **Explore → Loki**:

- By **trace**: `{service=~".+"} | json | traceId="<paste>"`
- By **span**: `{service=~".+"} | json | spanId="<paste>"`
- By **order**: `{service=~".+"} | json | orderId="<full-uuid>"`

**Grafana**

- Use **Explore → Loki** with the LogQL examples above (optionally narrow `{service="order-service"}` etc.).
- Provisioned dashboard **Commerce — ERROR logs** focuses on ERROR-level lines across services.
- In log rows (Explore), derived fields **TraceID** (opens Jaeger), **OrderId**, **SpanId** regex-match the JSON for quick copies when those links are configured.

**Jaeger → logs:** related logs run a **substring** query so the trace id matches inside the JSON line without requiring Loki labels.

> **Note:** Avoid promoting **`trace_id` / `span_id`** to Loki labels in real deployments — cardinality explodes. This POC follows the same pattern: structured logs + **`| json`** / integrations that filter by trace id in the line body.

## 5. Teardown
To stop all services and remove the network:

```bash
docker compose down
```

To also remove volumes (wiping the database, rabbitmq, loki data):

```bash
docker compose down -v
```

## 6. Known Limitations
- EOL Spring Boot 3.0.9 used for demonstration only (Dynatrace POC: **OneAgent Java** + Micrometer on **3.0.9**; see [DYNATRACE-POC.md](DYNATRACE-POC.md)).
- Local OpenTelemetry Collector used instead of SolarWinds. See [ARCHITECTURE.md](ARCHITECTURE.md) §22 for migration instructions.
