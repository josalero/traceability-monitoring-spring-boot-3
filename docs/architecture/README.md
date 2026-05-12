# Architecture — service documents

These pages supplement the main **[architecture overview](../ARCHITECTURE.md)** with **service-specific** sketches (REST, Stream consumers, YAML excerpts, and operational notes). Cross-cutting topics — saga overview, RabbitMQ topology, observability, Compose, UI, migration — stay in the overview.

**End-to-end outcomes:** [Scenarios, failures, and corner cases](../scenarios-and-edge-cases.md).

| Document | Focus |
|----------|--------|
| [order-service.md](order-service.md) | Redis cache for order reads, REST API, publishing `order.created`, order-status consumers |
| [inventory-service.md](inventory-service.md) | `order.created` consumer, idempotency, binder / DLQ configuration |
| [payment-service.md](payment-service.md) | `inventory.reserved` consumer, charge sketch, bindings |
| [notification-service.md](notification-service.md) | Terminal typed consumers and YAML |

Sketches may drift slightly from the codebase; treat paths and class names in Git as the source of truth.
