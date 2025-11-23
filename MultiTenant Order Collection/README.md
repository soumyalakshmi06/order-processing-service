# Bruno Collection for Multi-Tenant Order Processing Service

## Requests Included
- Create Order - Tenant A
- Create Order - Tenant B
- Get Order
- Poll Order Status (auto loops until processed)

## Usage
1. Run Spring Boot app on port 8080
2. Import this Bruno collection
3. Set environment to `local`
4. Run:
    - create-order.bru → saves orderId globally
    - get-order.bru → retrieves order
    - poll-order-status.bru → checks saga outcome
