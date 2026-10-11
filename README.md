<img width="2560" height="1403" alt="image" src="https://github.com/user-attachments/assets/c5e6ec20-f754-4bd6-8fb2-2abace56c6f2" />


# Online Banking Platform

A full-stack digital banking application built with **Spring Boot, Angular, PostgreSQL, Keycloak, RabbitMQ, and Docker**, deployed on **AWS EC2**.

The project uses a microservices architecture to separate account management, transaction processing, and notifications into independent services.

The application supports account creation, money transfers, transaction history, and real-time notifications.

**Live Demo:** https://paulsbank.duckdns.org

The public demo runs on an AWS EC2 instance and may be unavailable when the instance is stopped.

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

The **API Gateway** is the main entry point for frontend API requests and handles routing, authentication, CORS, and circuit-breaker fallback behavior.

In the AWS environment, **Caddy** acts as a reverse proxy in front of the application, providing HTTPS and routing external traffic to the appropriate services.

## Services

| Service | Responsibility |
| --- | --- |
| `api-gateway` | Routes external requests to backend services |
| `account-service` | Manages customer accounts and account operations |
| `transaction-service` | Processes transfers and transaction history |
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

- authentication
- role management
- token generation
- token validation

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

- centralized routing
- JWT authentication
- CORS configuration
- WebSocket routing
- circuit breakers
- fallback handling

### HTTPS and reverse proxy

The AWS deployment uses **Caddy** as a reverse proxy to provide a single HTTPS entry point.

Incoming requests are routed based on their paths:

- Frontend requests are forwarded to Angular.
- `/api/*` requests are forwarded to the API Gateway.
- `/ws/*` requests are forwarded to the API Gateway for WebSocket communication.
- Keycloak authentication requests are forwarded to the Keycloak service.

Backend services and databases are not directly exposed to the public internet.

This separates external HTTP routing from internal service communication.

## Technology Stack

| Area | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3 |
| API Gateway | Spring Cloud Gateway |
| Frontend | Angular |
| Database | PostgreSQL |
| Authentication | Keycloak, OAuth2/OIDC, JWT |
| Messaging | RabbitMQ |
| Real-time communication | WebSocket / STOMP |
| Resilience | Resilience4j |
| Containers | Docker, Docker Compose |
| Cloud Infrastructure | AWS EC2 |
| Reverse Proxy / HTTPS | Caddy |
| DNS | DuckDNS |
| CI | GitHub Actions |
| Build | Maven, npm |

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
│       ├── Dockerfile
│       └── src/
│           └── environments/
│               ├── environment.ts
│               └── environment.aws.ts
│
├── infrastructure/
│   ├── caddy/
│   │   └── Caddyfile
│   ├── docker/
│   │   ├── docker-compose.yml
│   │   └── docker-compose.aws.yml
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

- Docker
- Docker Compose

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

| Component | URL |
| --- | --- |
| Web Application | `http://localhost:4200` |
| API Gateway | `http://localhost:8080` |
| Account Service | `http://localhost:8081` |
| Transaction Service | `http://localhost:8082` |
| Notification Service | `http://localhost:8083` |
| Keycloak | `http://localhost:8180` |
| RabbitMQ Management | `http://localhost:15672` |

## AWS Deployment

The application is deployed on a single **AWS EC2 instance running Amazon Linux 2023**, with Docker Compose managing the application containers.

The deployment uses the same microservices as the local environment, with additional configuration for HTTPS, authentication, and external access.

### Deployment Architecture

```text
                        Internet
                           │
                           ▼
                paulsbank.duckdns.org
                           │
                           ▼
                    HTTPS :443
                           │
                           ▼
                 ┌─────────────────┐
                 │      Caddy      │
                 │  Reverse Proxy  │
                 └────────┬────────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
          ▼               ▼               ▼
      Angular SPA     API Gateway      Keycloak
          :80           :8080           :8080
                          │
              ┌───────────┼───────────┐
              │           │           │
              ▼           ▼           ▼
           Account    Transaction  Notification
           Service      Service      Service
              │           │           │
              ▼           ▼           ▼
          PostgreSQL  PostgreSQL  PostgreSQL
                          │
                       RabbitMQ
```

All application components run as Docker containers within the EC2 instance.

Caddy handles incoming HTTPS traffic, while service-to-service communication uses the internal Docker network.

### Docker Compose Configuration

The deployment uses two Compose files:

- `docker-compose.yml` defines the application services and shared infrastructure.
- `docker-compose.aws.yml` adds AWS-specific configuration, including local port bindings, HTTPS issuer settings, and the Caddy reverse proxy.

With the EC2 instance, DNS, service credentials, and Keycloak configuration prepared, the application can be started using:

```bash
docker compose \
  -f infrastructure/docker/docker-compose.yml \
  -f infrastructure/docker/docker-compose.aws.yml \
  up -d --build
```

The AWS configuration keeps internal service ports bound to the EC2 host's loopback interface.

Only HTTP and HTTPS are exposed publicly through the EC2 security group.

### Environment Configuration

The Angular frontend supports separate configurations for local development and AWS deployment.

| Environment | Configuration | API Endpoint |
| --- | --- | --- |
| Local | `environment.ts` | `http://localhost:8080` |
| AWS | `environment.aws.ts` | `https://paulsbank.duckdns.org` |

The Dockerfile supports a configurable Angular build.

The AWS Docker Compose configuration uses:

```text
production,aws
```

This enables production build optimization while replacing the local environment settings with AWS-specific URLs.

The same codebase can therefore be used for local development and cloud deployment without manually changing API endpoints.

### Authentication

Keycloak is configured to use the public HTTPS domain as its OAuth2/OIDC issuer.

The Angular application uses the Authorization Code Flow, and backend services validate JWT access tokens against the configured issuer.

The Keycloak client also requires the corresponding HTTPS redirect URI to be registered.

### HTTPS

DuckDNS provides a public DNS name for the EC2 instance.

Caddy manages HTTPS certificates and forwards requests to the appropriate Docker services.

The deployment uses:

- port `80` for HTTP and HTTPS redirection
- port `443` for HTTPS access
- internal Docker networking for service communication

The public Keycloak administration endpoint is blocked by the Caddy routing configuration.

This AWS deployment is intended for demonstration and uses test data only.

## What This Project Demonstrates

This project focuses on practical backend, distributed-system, and cloud deployment concepts:

- microservices architecture
- service-owned databases
- REST-based service communication
- event-driven messaging
- OAuth2/OIDC authentication
- API Gateway patterns
- circuit-breaker fault handling
- real-time WebSocket notifications
- Docker-based local environments
- AWS EC2 deployment
- HTTPS reverse proxy configuration
- environment-specific application builds
- CI workflows

The goal of the project is to demonstrate how these patterns can be combined and deployed as a banking-style application.
