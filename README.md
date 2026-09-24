
<img width="2560" height="1298" alt="image" src="https://github.com/user-attachments/assets/8750a065-f367-4df3-9176-14d0014f408f" />

# Online Banking Platform

A full-stack digital banking application built with **Spring Boot, Angular, PostgreSQL, Keycloak, RabbitMQ, and Docker**.

The project uses a microservices architecture to separate account management, transaction processing, and notifications into independent services.

## Architecture

```text
                         ┌─────────────────┐
                         │   Angular SPA   │
                         │      :4200      │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   API Gateway   │
                         │      :8080      │
                         └───────┬─────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
              ▼                  ▼                  ▼
      Account Service    Transaction Service   Notification Service
          :8081               :8082                :8083
              │                  │                  │
              ▼                  ▼                  ▼
         account_db       transaction_db      notification_db
              │                  │
              └────── RabbitMQ ──┴───────────────► Notifications
```

**Keycloak** provides centralized authentication and authorization using OAuth2/OIDC and JWT.

The **API Gateway** is the main entry point for frontend requests and handles routing, authentication, CORS, and circuit-breaker fallback behavior.

## Services

| Service                | Responsibility                                       |
| ---------------------- | ---------------------------------------------------- |
| `api-gateway`          | Routes external requests to backend services         |
| `account-service`      | Manages customer accounts and account operations     |
| `transaction-service`  | Processes transfers and transaction history          |
| `notification-service` | Consumes events and delivers real-time notifications |

## Key Design Decisions

### Microservices instead of a monolith

Account management, transactions, and notifications are separated into independent services.

This keeps domain responsibilities clear and allows each service to be deployed and scaled independently.

The trade-off is additional infrastructure and service communication complexity.

### Database per service

Each business service owns its own PostgreSQL database.

```text
account-service       -> account_db
transaction-service   -> transaction_db
notification-service  -> notification_db
```

Services do not directly access another service's database.

Cross-service communication happens through REST APIs or events, which keeps service boundaries clear and reduces database-level coupling.

### Keycloak for authentication

Authentication is handled by **Keycloak** instead of implementing custom JWT authentication inside each service.

The frontend authenticates through OAuth2/OIDC, and backend services validate JWT access tokens.

This provides centralized:

* authentication
* role management
* token generation
* token validation

### REST for synchronous operations

REST communication is used when a service requires an immediate result before continuing.

For example, transaction processing may need account information before completing a transfer.

```text
transaction-service
        │
        │ REST
        ▼
 account-service
```

### RabbitMQ for asynchronous events

RabbitMQ is used for operations that do not need to block the main request.

For example:

```text
account-service
      │
      └── account.created ───────┐
                                 ▼
                          notification-service

transaction-service
      │
      └── transaction.created ───┘
```

This prevents notification processing from becoming part of the critical transaction path.

It also makes it easier to add future consumers such as audit, analytics, or fraud-monitoring services.

### API Gateway

Clients communicate with backend services through Spring Cloud Gateway instead of calling each service directly.

The gateway provides:

* centralized routing
* JWT authentication
* CORS configuration
* WebSocket routing
* circuit breakers
* fallback handling

## Technology Stack

| Area                    | Technology                 |
| ----------------------- | -------------------------- |
| Backend                 | Java 21, Spring Boot 3     |
| API Gateway             | Spring Cloud Gateway       |
| Frontend                | Angular                    |
| Database                | PostgreSQL                 |
| Authentication          | Keycloak, OAuth2/OIDC, JWT |
| Messaging               | RabbitMQ                   |
| Real-time communication | WebSocket / STOMP          |
| Resilience              | Resilience4j               |
| Containers              | Docker, Docker Compose     |
| CI                      | GitHub Actions             |
| Build                   | Maven, npm                 |

## Project Structure

```text
online-banking-main/
│
├── backend/
│   ├── api-gateway/
│   ├── account-service/
│   ├── transaction-service/
│   └── notification-service/
│
├── frontend/
│   └── web/
│
├── infrastructure/
│   ├── docker/
│   └── keycloak/
│
├── .github/
│   └── workflows/
│
├── scripts/
├── Makefile
└── README.md
```

## Running Locally

### Requirements

* Docker
* Docker Compose

### Start the application

```bash
docker compose -f infrastructure/docker/docker-compose.yml up -d --build
```

Or use the Makefile:

```bash
make build
```

Check running containers:

```bash
make ps
```

View logs:

```bash
make logs
```

Stop the environment:

```bash
make down
```

## Local Services

| Component            | URL                      |
| -------------------- | ------------------------ |
| Web Application      | `http://localhost:4200`  |
| API Gateway          | `http://localhost:8080`  |
| Account Service      | `http://localhost:8081`  |
| Transaction Service  | `http://localhost:8082`  |
| Notification Service | `http://localhost:8083`  |
| Keycloak             | `http://localhost:8180`  |
| RabbitMQ Management  | `http://localhost:15672` |

## What This Project Demonstrates

This project focuses on practical backend and distributed-system concepts:

* microservices architecture
* service-owned databases
* REST-based service communication
* event-driven messaging
* OAuth2/OIDC authentication
* API Gateway patterns
* circuit-breaker fault handling
* real-time WebSocket notifications
* Docker-based local environments
* CI workflows

The goal of the project is to demonstrate how these patterns can be combined in a banking-style application.
