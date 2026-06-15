# Microservices Project

A Spring Boot 3.3 microservices demo with JWT authentication, service discovery, API gateway, circuit breaker, and H2 databases.

## Architecture

```
                    ┌─────────────────┐
                    │  Service Registry│
                    │  (Eureka) :8761  │
                    └────────┬────────┘
                             │ registers to
    ┌───────────────┐  ┌─────┴──────┐  ┌───────────────┐
    │  API Gateway  │  │   Order    │  │   Payment    │
    │ (Spring Cloud)│──│  Service   │──│   Service    │
    │    :8080      │  │   :8081    │  │   :8082      │
    └───────────────┘  └────────────┘  └──────────────┘
                              │
                         circuit breaker
                         (Resilience4j)
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| `service-registry` | 8761 | Eureka Server — all services register here |
| `api-gateway` | 8080 | Spring Cloud Gateway — single entry point |
| `order-service` | 8081 | Order management with JWT auth + circuit breaker |
| `payment-service` | 8082 | Payment processing with JWT auth |

## Quick Start

### Using Docker

```bash
docker compose up --build
```

### Using Java directly

Build all JARs:
```bash
cd order-service && mvn clean package -DskipTests
cd ../payment-service && mvn clean package -DskipTests
cd ../service-registry && mvn clean package -DskipTests
cd ../api-gateway && mvn clean package -DskipTests
```

Start in order:
```bash
java -jar service-registry/target/service-registry-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
java -jar payment-service/target/payment-service-1.0.0.jar
java -jar order-service/target/order-service-1.0.0.jar
```

## Authentication

Dummy users seeded on startup:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `password` | ADMIN |
| `user` | `123456` | USER |

### Login

```bash
curl -s http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

Response:
```json
{"token":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTc0OTU5MDk0MCwiZXhwIjoxNzQ5NTk0NTQwfQ.bdQF..."}
```

Tokens are stored in the `tokens` table and validated against the DB on each request.

### Using the token

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -s http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"item":"laptop","quantity":1}'
```

Response:
```json
{"orderId":"1","order":"Order created for laptop","payment":"Payment successful"}
```

## Endpoints

### Open (no auth)
| Method | Path | Service |
|--------|------|---------|
| POST | `/auth/login` | order-service |
| GET | `/h2-console/**` | order-service, payment-service |

### Protected (JWT required)
| Method | Path | Service |
|--------|------|---------|
| POST | `/orders` | order-service |
| POST | `/payments` | payment-service |
| GET | `/actuator/health` | all services |
| GET | `/actuator/info` | order-service, payment-service |
| GET | `/actuator/circuitbreakers` | order-service |

## API via Gateway

All requests go through `http://localhost:8080`:

- `POST /auth/login` → forwarded to order-service
- `POST /orders` → forwarded to order-service
- `POST /payments` → forwarded to payment-service

## Features

### Circuit Breaker (Resilience4j)
Order Service wraps the payment call with a circuit breaker. If payment-service is down:
- The order is saved as `PENDING`
- A fallback message is returned
- Circuit breaker health visible at `/actuator/circuitbreakers`

### Service Discovery (Eureka)
- All services register with Eureka at startup
- Order Service uses `@LoadBalanced RestTemplate` to call `http://payment-service/payments`
- Gateway routes via `lb://order-service` / `lb://payment-service`

### Database (H2 in-memory)
Each service has its own H2 database:

**Order Service** — `jdbc:h2:mem:ordersdb`
- `users` — dummy users
- `tokens` — issued JWT tokens
- `orders` — created orders

**Payment Service** — `jdbc:h2:mem:paymentsdb`
- `users` — dummy users
- `tokens` — issued JWT tokens
- `payments` — processed payments

### H2 Console
Access at:
- `http://localhost:8081/h2-console` (JDBC URL: `jdbc:h2:mem:ordersdb`)
- `http://localhost:8082/h2-console` (JDBC URL: `jdbc:h2:mem:paymentsdb`)

Login: `sa` / empty password

## Project Structure

```
example-microservices/
├── service-registry/          # Eureka Server
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/.../ServiceRegistryApplication.java
│
├── api-gateway/               # Spring Cloud Gateway
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/resources/application.yml  (route config)
│
├── order-service/             # Order management
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/example/orderservice/
│       ├── OrderServiceApplication.java
│       ├── config/     (SecurityConfig, AppConfig, DataInitializer)
│       ├── controller/ (AuthController, OrderController)
│       ├── dto/        (LoginRequest, LoginResponse, OrderRequest)
│       ├── entity/     (User, Token, Order)
│       ├── repository/ (UserRepository, TokenRepository, OrderRepository)
│       ├── security/   (JwtAuthFilter)
│       ├── service/    (AuthService, OrderService)
│       └── util/       (JwtUtil)
│
├── payment-service/           # Payment processing
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/example/paymentservice/
│       ├── PaymentServiceApplication.java
│       ├── config/     (SecurityConfig, DataInitializer)
│       ├── controller/ (AuthController, PaymentController)
│       ├── dto/        (LoginRequest, LoginResponse)
│       ├── entity/     (User, Token, Payment)
│       ├── repository/ (UserRepository, TokenRepository, PaymentRepository)
│       ├── security/   (JwtAuthFilter)
│       ├── service/    (AuthService, PaymentService)
│       └── util/       (JwtUtil)
│
├── docker-compose.yml
└── README.md
```
