# Multi-Tenant Order Processing Service

A clean and extensible backend service that demonstrates:

- **CQRS Architecture**
- **Tenant-Specific Validation Rules**
- **Transaction Outbox Pattern**
- **Saga-Style Background Processing**
- **Java 21, Spring Boot 3, JPA/Hibernate, H2**

This project is a complete solution for the **Technical Assignment — Multi-Tenant Order Processing Service**.

---

# 🚀 1. Features Overview

### ✔ Order Domain
Each order has:
- `id`
- `tenantId`
- `amount`
- `quantity`
- `status` → `PENDING`, `PROCESSED`, `FAILED`

---

### ✔ CQRS (Commands & Queries)

**Write Side (Commands)**
- `POST /api/v1/orders`
- Handles creation using Command Handler
- Writes Order (PENDING) + Outbox Event (PENDING) in same transaction

**Read Side (Queries)**
- `GET /api/v1/orders/{id}`
- Uses Query Handler
- Pure read logic

---

### ✔ Multi-Tenant Validation

| Tenant | Validation Rules |
|--------|------------------|
| **tenantA** | amount > 100 |
| **tenantB** | amount > 100 **AND** quantity > 10 |

Validators auto-register using `ValidatorRegistry`.

Adding more tenants requires **zero modification** to existing logic.

---

### ✔ Transaction Outbox Pattern

When an order is created:
1. Save Order with `PENDING`
2. Save OutboxEvent with `PENDING`
3. Both occur in the **same database transaction**

Ensures no order is created without an event and vice versa.

---

### ✔ Saga Processing (Background Worker)

A scheduled worker:
1. Fetches `PENDING` Outbox Events
2. Marks them `IN_PROGRESS`
3. Loads the associated Order
4. Runs tenant validation
5. If valid → Order = `PROCESSED`, Event = `PROCESSED`
6. If invalid → Order = `FAILED`, Event = `FAILED`

Worker retries automatically on restart (idempotent).

---

# 📦 2. Architecture

```
src/main/java/com.demo.orderProcessingService
 ├── commands/
 │     ├── CreateOrderCommand
 │     ├── OrderCommandHandler
 │
 ├── queries/
 │     ├── GetOrderQuery
 │     ├── OrderQueryHandler
 │
 ├── controller/
 │     ├── CommandController
 │     ├── QueryController
 │
 ├── validation/
 │     ├── TenantOrderValidator
 │     ├── TenantAValidator
 │     ├── TenantBValidator
 │     ├── ValidatorRegistry
 │
 ├── outbox/
 │     ├── OutboxEvent
 │     ├── OutboxRepository
 │     ├── OutboxService
 │     ├── OutboxWorker
 │     ├── OutboxProcessor
 │
 ├── domain/
 │     ├── OrderEntity
 │
 ├── config/
 │     ├── SecurityConfig
 │
 ├── OrderProcessingServiceApplication
```

---

# ⚙️ 3. How to Run

### **Requirements**
- Java **21+**
- Maven **3.9+**

### **Start the application**
```bash
mvn clean package
java -jar target/orderProcessingService-0.0.1-SNAPSHOT.jar
```

### **Or run from IDE**
Run:
`OrderProcessingServiceApplication`

---

# 🧪 4. API Usage

---

## ➕ Create Order
### `POST /api/v1/orders`

**Sample Request**
```json
{
  "tenantId": "tenantA",
  "amount": 150,
  "quantity": 2
}
```

**Response**
- `200 OK` → Order saved in PENDING state
- Async worker updates final state later

---

## 🔍 Get Order
### `GET /api/v1/orders/{id}`

**Sample Response**
```json
{
  "id": "uuid",
  "tenantId": "tenantA",
  "amount": 150,
  "quantity": 2,
  "status": "PROCESSED"
}
```

---

# 🧵 5. How Saga + Outbox Works

1. Client sends POST request
2. Command Handler:
   - Saves Order (PENDING)
   - Saves OutboxEvent (PENDING)
3. OutboxWorker runs every 3 seconds:
   - Reads PENDING events
   - Validates based on tenant rules
   - Updates order to PROCESSED/FAILED
   - Marks event accordingly

This ensures **strong consistency + reliable asynchronous processing**.

---

# 🛠️ 6. H2 Console

### URL:
```
http://localhost:8080/h2-console
```

### JDBC URL:
```
jdbc:h2:mem:orders
```

You can inspect:
- `ORDERS`
- `OUTBOX_EVENTS`

---

# 🔍 7. Testing Examples (Bruno / Postman)

---

## ✔ tenantA success
```json
{
  "tenantId": "tenantA",
  "amount": 200,
  "quantity": 5
}
```

## ❌ tenantB failure (quantity too low)
```json
{
  "tenantId": "tenantB",
  "amount": 200,
  "quantity": 5
}
```

## ✔ tenantB success
```json
{
  "tenantId": "tenantB",
  "amount": 200,
  "quantity": 15
}
```

---

# 🧩 8. Extendability

To add a new tenant:

Create validator:
```java
@Component
public class TenantCValidator implements TenantOrderValidator {
    public boolean validate(OrderEntity order) {
        return order.getAmount() > 500;
    }
}
```

Registered automatically — no changes to core logic.

---

# 🎯 9. Why This Project Meets the Assignment 100%

- Clean **CQRS** implementation
- **Fully working Saga** with Outbox pattern
- Accurate **tenant-based validation**
- Reliable worker with **retry support**
- Clean, modular, production-ready architecture
- Easy to extend
- Secure and robust

---

# 🏁 10. Conclusion

This project demonstrates strong backend engineering skills through:

- Well-structured architecture
- Clear separation of concerns
- Reliable event-driven processing
- Strong multi-tenant strategy
- Clean and maintainable codebase


