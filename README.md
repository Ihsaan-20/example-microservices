# Microservices Project

A Spring Boot 3.3 microservices demo with centralized JWT authentication (auth-service), service discovery (Eureka), API gateway, circuit breaker (Resilience4j), and MySQL databases.

## Architecture

```
                        ┌─────────────────┐
                        │  Service Registry│
                        │  (Eureka) :8761  │
                        └────────┬────────┘
                                 │ registers to
        ┌───────────────┐  ┌─────┴──────┐  ┌───────────────┐
        │  API Gateway  │  │   Auth     │  │   Order      │
        │ (Spring Cloud)│  │  Service   │  │   Service    │
        │    :8080      │──│   :8083    │──│   :8081      │──┐
        └───────────────┘  └────────────┘  └──────┬───────┘  │
                                                   │         │
                                              circuit     Payment
                                              breaker    Service
                                              (Resil.4j)  :8082
                                                    │      │
                                                    └──────┘
```

## Services

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| `service-registry` | 8761 | — | Eureka Server — all services register here |
| `api-gateway` | 8080 | — | Spring Cloud Gateway — single entry point |
| `auth-service` | 8083 | `ex_mic_auth_db` | JWT login & token validation |
| `order-service` | 8081 | `ex_mic_order_service` | Order management with circuit breaker |
| `payment-service` | 8082 | `ex_mic_payment_service` | Payment processing |

## Quick Start

### Using Docker

```bash
docker compose up --build
```

### Using Java directly

Build all JARs:
```bash
cd service-registry && mvn clean package -DskipTests
cd ../auth-service && mvn clean package -DskipTests
cd ../api-gateway && mvn clean package -DskipTests
cd ../payment-service && mvn clean package -DskipTests
cd ../order-service && mvn clean package -DskipTests
```

Make sure MySQL is running on `localhost:3306` with root/root, then start in order:
```bash
java -jar service-registry/target/service-registry-1.0.0.jar
java -jar auth-service/target/auth-service-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
java -jar payment-service/target/payment-service-1.0.0.jar
java -jar order-service/target/order-service-1.0.0.jar
```

## Authentication

Auth is centralized in auth-service (port 8083). Dummy users seeded on startup:

| Username | Password |
|----------|----------|
| `admin` | `password` |
| `user` | `123456` |

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

Tokens are stored in the `tokens` table in auth-service and validated against the DB on each request via auth-service.

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
| POST | `/auth/login` | auth-service |

### Protected (JWT required)
| Method | Path | Service |
|--------|------|---------|
| POST | `/auth/validate` | auth-service |
| POST | `/orders` | order-service |
| POST | `/payments` | payment-service |
| GET | `/actuator/health` | all services |
| GET | `/actuator/circuitbreakers` | order-service |

## API via Gateway

All requests go through `http://localhost:8080`:

- `POST /auth/login` → forwarded to auth-service
- `POST /auth/validate` → forwarded to auth-service
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
- Gateway routes via `lb://<service-name>`

### Database (MySQL)

Each service has its own MySQL database (all on the same MySQL instance, port 3306):

| Service | Database |
|---------|----------|
| auth-service | `ex_mic_auth_db` |
| order-service | `ex_mic_order_service` |
| payment-service | `ex_mic_payment_service` |

Tables are auto-created by JPA/Hibernate (`ddl-auto: update`).

## Project Structure

```
example-microservices/
├── service-registry/          # Eureka Server
├── auth-service/              # Centralized authentication
│   ├── entity/   (User, Token)
│   ├── repository/
│   ├── service/
│   ├── controller/ (AuthController)
│   └── security/  (SecurityConfig — no JWT filter)
│
├── api-gateway/               # Spring Cloud Gateway
│
├── order-service/             # Order management
│   ├── entity/   (Order)
│   ├── repository/ (OrderRepository)
│   ├── controller/ (OrderController)
│   ├── security/  (JwtAuthFilter — local JWT validation)
│   └── service/   (OrderService with circuit breaker)
│
├── payment-service/           # Payment processing
│   ├── entity/   (Payment)
│   ├── repository/ (PaymentRepository)
│   ├── controller/ (PaymentController)
│   ├── security/  (JwtAuthFilter — local JWT validation)
│   └── service/   (PaymentService)
│
├── docker-compose.yml
└── README.md
```
