# Order Service

A Spring Boot 3 microservice for managing food delivery orders.

## Tech Stack

- Java 17
- Spring Boot 3.3.0
- Spring Web, Spring Data JPA, Spring Validation, Spring Actuator
- PostgreSQL
- Lombok
- Maven

## Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL 13+

## Database Setup

```sql
CREATE DATABASE order_db;
```

> The service uses `spring.jpa.hibernate.ddl-auto=update`, so tables are created automatically on startup.

## Configuration

Edit `src/main/resources/application.yml` if your PostgreSQL credentials differ:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/order_db
    username: postgres
    password: postgres
```

## Running the Service

```bash
# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run
```

The service starts on **http://localhost:8083**.

## API Endpoints

### 1. Create Order

```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "restaurantId": 1,
    "items": [
      {
        "menuItemId": 1,
        "itemName": "Margherita Pizza",
        "quantity": 2,
        "price": 14.99
      }
    ]
  }'
```

### 2. Get Order by ID

```bash
curl http://localhost:8083/api/orders/1
```

### 3. Get All Orders for a User

```bash
curl http://localhost:8083/api/orders/user/1
```

### 4. Update Order Status

```bash
curl -X PUT http://localhost:8083/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "CONFIRMED"}'
```

Valid statuses: `CREATED`, `PAYMENT_PENDING`, `PAYMENT_COMPLETED`, `CONFIRMED`, `CANCELLED`, `DELIVERED`

### 5. Cancel Order

```bash
curl -X DELETE http://localhost:8083/api/orders/1
```

> Sets order status to `CANCELLED`. Orders in `DELIVERED` status cannot be cancelled.

## Actuator

```bash
# Health check
curl http://localhost:8083/actuator/health

# Info
curl http://localhost:8083/actuator/info
```

## Project Structure

```
src/main/java/com/fooddelivery/orderservice/
├── OrderServiceApplication.java
├── config/
│   └── WebConfig.java
├── controller/
│   └── OrderController.java
├── dto/
│   ├── CreateOrderRequest.java
│   ├── ErrorResponse.java
│   ├── OrderItemRequest.java
│   ├── OrderItemResponse.java
│   ├── OrderResponse.java
│   └── UpdateOrderStatusRequest.java
├── entity/
│   ├── Order.java
│   ├── OrderItem.java
│   └── OrderStatus.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── InvalidOrderException.java
│   └── ResourceNotFoundException.java
├── repository/
│   ├── OrderItemRepository.java
│   └── OrderRepository.java
└── service/
    ├── OrderService.java
    └── impl/
        └── OrderServiceImpl.java
```

## Business Rules

- User ID and Restaurant ID are required references (validated via external services in future).
- An order must have at least one item.
- Quantity must be ≥ 1; Price must be > 0.
- Total amount = Σ (quantity × price) per item.
- New orders start with status `CREATED`.
- Cancel (`DELETE /api/orders/{id}`) sets status to `CANCELLED` — the record is **not** deleted.
- Orders with status `DELIVERED` **cannot** be cancelled.

## Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "message": "Order not found with id: 99",
  "path": "/api/orders/99"
}
```

## Future Enhancements (Planned)

- Feign Client integration to validate User and Restaurant IDs
- Kafka event publishing (order created, status changed)
- Payment service integration
- JWT security
