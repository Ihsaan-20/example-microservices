# Example Microservices Project

## Overview

This project contains a microservices-based application with:

* Order Service
* Payment Service
* JWT Authentication
* Docker Compose setup
* H2 Database Console

## Project Structure

```
example-microservices
│
├── order-service
│   └── target
│       └── order-service-1.0.0.jar
│
├── payment-service
│   └── target
│       └── payment-service-1.0.0.jar
│
├── docker-compose.yml
└── README.md
```

---

## Prerequisites

Make sure you have installed:

* Java 17+
* Maven
* Docker
* Docker Compose

Verify installation:

```bash
java -version
docker --version
docker compose version
```

---

# Running Application with Docker

Build and start all services:

```bash
docker-compose up --build
```

This will:

* Build Docker images
* Start all microservices
* Create required containers

---

# Running Services Manually

Navigate to target directory:

```bash
cd target
```

## Start Payment Service

```bash
java -jar payment-service-1.0.0.jar --server.port=8082
```

Payment Service will run on:

```
http://localhost:8082
```

---

## Start Order Service

```bash
java -jar order-service-1.0.0.jar --server.port=8081
```

Order Service will run on:

```
http://localhost:8081
```

---

# Authentication

Generate JWT Token:

```bash
curl -X POST http://localhost:8081/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"user","password":"pass"}'
```

Example Response:

```json
{
  "token": "your-jwt-token"
}
```

Copy the token and use it for secured APIs.

---

# Order API

Create Order:

```bash
curl -X POST http://localhost:8081/orders \
-H "Authorization: Bearer <your-jwt-token>" \
-H "Content-Type: application/json" \
-d '{"orderId":1,"amount":500}'
```

---

# H2 Database Console

## Order Service Database

Open:

```
http://localhost:8081/h2-console
```

## Payment Service Database

Open:

```
http://localhost:8082/h2-console
```

### H2 Console Configuration

JDBC URL:

```
jdbc:h2:mem:testdb
```

Username:

```
sa
```

Password:

```
(empty)
```

---

# Service Ports

| Service         | Port |
| --------------- | ---- |
| Order Service   | 8081 |
| Payment Service | 8082 |

---

# Stop Application

Docker:

```bash
docker-compose down
```

Manual run:

Press:

```
CTRL + C
```

---

# Notes

* Make sure ports `8081` and `8082` are free before starting.
* JWT token is required for protected APIs.
* H2 database is running in memory mode, data will reset after restart.
