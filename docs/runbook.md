# Commerce POC Runbook

## 1. Prerequisites
- Docker and Docker Compose
- Java 17
- Maven 3.8+

## 2. Startup
Run from the repository root:

```bash
docker compose up -d --build
```

The gateway exposes APIs at `http://localhost:8080` and the UI at `http://localhost:5173`.

## 3. Observability
This repository now maintains a **Dynatrace-only** path:

- **Distributed traces:** OpenTelemetry Java agent → Dynatrace OTLP
- **Metrics:** Micrometer → Dynatrace Metrics API

Before startup, provide the values documented in [DYNATRACE-POC.md](DYNATRACE-POC.md):

- `DT_ENVIRONMENT_ID`
- `DYNATRACE_API_TOKEN`
- `DT_OTLP_ENDPOINT`
- `DT_OTLP_TRACE_TOKEN`

Then generate traffic through the gateway and validate traces in Dynatrace **Distributed traces**.

## 4. Teardown

```bash
docker compose down
```

To also remove named volumes:

```bash
docker compose down -v
```

## 5. Known limitations
- Spring Boot 3.0.9 is EOL and used for demonstration only.
- This repo intentionally does **not** maintain a local Jaeger / Prometheus / Loki / Grafana stack anymore.
