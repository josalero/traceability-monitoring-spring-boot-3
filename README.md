# Commerce POC — Traceability & Monitoring

This project is a Proof of Concept (POC) demonstrating a microservices architecture built with **Spring Boot 3.0.9**, with distributed tracing and metrics exported directly to Dynatrace.

| Stack item | Version / pin | Source |
|------------|-----------------|--------|
| Spring Boot | 3.0.9 | Root `pom.xml` (`spring-boot-starter-parent`) |
| Java | 17 | Root `pom.xml` (`java.version`) |
| Spring Cloud | 2022.0.5 | Root `pom.xml` (`spring-cloud.version`) |
| OpenTelemetry Java agent | 2.1.0 | `infra/otel/install-agent.sh` (`OTEL_VERSION`) |
| Dynatrace direct export (OTLP traces + Micrometer metrics) | See guide | [docs/DYNATRACE-POC.md](docs/DYNATRACE-POC.md) |

For the current system shape and observability path, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

**Flows and edge cases** (happy paths, inventory/payment failures, cache behavior, idempotency, POC payment randomness): [docs/scenarios-and-edge-cases.md](docs/scenarios-and-edge-cases.md).

## Dynatrace architecture options

There are four valid integration models for this project:

| Option | Best for | Tradeoff |
|---|---|---|
| [OneAgent](docs/dynatrace-javaagent/oneagent.md) | Lowest app-side effort and the most Dynatrace-native experience | Least portable |
| [OpenTelemetry Java agent + Dynatrace direct export](docs/dynatrace-javaagent/otel-java-agent-direct-dynatrace.md) | Strong auto-instrumentation with fewer moving parts | **Current implementation** |
| [OpenTelemetry Java agent + Collector + Dynatrace](docs/dynatrace-javaagent/otel-java-agent-collector-dynatrace.md) | Rich auto-instrumentation plus a vendor-neutral pipeline | More infrastructure to maintain |
| [Agentless custom SDK + Dynatrace](docs/dynatrace-javaagent/custom-sdk-dynatrace.md) | No runtime agent and maximum code-level control | Most engineering ownership |

For a side-by-side comparison, see [docs/dynatrace-javaagent/README.md](docs/dynatrace-javaagent/README.md).

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

2. **Start the stack:**
   This starts PostgreSQL, RabbitMQ, Redis, Eureka, Config Server, API Gateway, all domain services, and the React UI. JVM traces go directly to Dynatrace over OTLP; Micrometer metrics go directly to Dynatrace.
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

This repository now maintains a **Dynatrace-only** observability path:

- **Traces:** OpenTelemetry Java agent → Dynatrace OTLP
- **Metrics:** Micrometer → Dynatrace Metrics API
- **Logs:** application JSON logs remain on stdout for container/runtime collection

See [docs/DYNATRACE-POC.md](docs/DYNATRACE-POC.md) for the required environment variables, tokens, and validation steps.

## Teardown

```bash
docker compose down
```

To also remove named volumes:

```bash
docker compose down -v
```
