# Multi-Tenant Order Processing Service

## Overview
This sample Spring Boot application demonstrates:
- CQRS (Commands and Queries separation)
- Multi-tenant validation rules
- Saga-style processing via Transaction Outbox pattern
- Background worker that reliably processes outbox events without an external broker

## How to run
1. Build:
```bash
mvn clean package
```
2. Run:
```bash
java -jar target/multi-tenant-order-processing-0.0.1-SNAPSHOT.jar
```
3. API:
- POST `/api/v1/orders` to create an order
- GET `/api/v1/orders/{id}` to fetch order

## Folder structure
See repository root. Important packages:
- `commands` - write-side handlers
- `queries` - read-side handlers
- `validation` - tenant validators + registry
- `outbox` - outbox event entity, repo, worker

## Tenant validation (pluggable)
- `tenantA`: amount > 100
- `tenantB`: amount > 100 and quantity > 10
Add new tenant by adding a `@Component("<tenantId>")` implementing `TenantOrderValidator`.

## Outbox + Saga flow 

Client -> POST /api/v1/orders
  |
  v
Command Handler (DB Transaction)
  - save Order (PENDING)
  - save OutboxEvent (PENDING)
  (COMMIT)
  |
  v
Outbox table (PENDING events)
  |
  v (OutboxWorker polls)
Worker:
  - claim event (CAS -> IN_PROGRESS)
  - process event: load order, pick tenant validator, validate:
      - valid -> order.PROCESSED
      - invalid -> order.FAILED
  - mark outbox PROCESSED

## Example curl
Create order (tenantA):
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"tenantA","amount":150.0,"quantity":2}'
```

Get order:
```bash
curl http://localhost:8080/api/v1/orders/<orderId>
```

## Tests included
- basic CommandHandler + Outbox flow tests
- Validator registry test
- Worker integration-style test (uses H2)

## Notes & Improvements
- Use JSON (Jackson) for payloads parsing instead of naive string extraction.
- Improve OutboxRepository CAS semantics for large-scale concurrency.
- Add monitoring/metrics and an admin API to reprocess/inspect failed events.

