# TaskMaster — Multi-Tenant Task Management Platform

> **All APIs are versioned under `/api/v1/`**

A production-grade, microservices-based task management application built with Kotlin/Spring Boot, Angular, Keycloak, Kafka, and a full observability stack. Designed for multi-tenant SaaS use cases.

---

## Architecture at a Glance

```
                           ┌─────────────────────────┐
        User App (4200) ──▶│                         │
                           │      API Gateway         │──▶ Eureka (service discovery)
     Admin App (4201) ──▶│      :8080               │
                           │   JWT validation         │
                           │   Tenant header inject   │──▶ Config Server (:8888)
                           └─────────┬───────────────┘
                                     │ lb:// routing
          ┌──────────────────────────┼────────────────────────┐
          ▼                          ▼                         ▼
   user-service (:8081)     task-service (:8082)    notification-service (:8083)
   • Tenants/Users           • Task CRUD              • Kafka consumer
   • Roles/Permissions       • Spring GraphQL         • Email (Mailhog)
   • Keycloak Admin          • Outbox relay           • FCM push
                                     │                         ▲
                                     │ Kafka Outbox            │
                                     ▼                   scheduler-service (:8084)
                               [notification-events]     • Quartz scheduling
                                                         • Feign → task/notification
```

**Infrastructure:** PostgreSQL 17 · Redis 7 · Keycloak 26 · Kafka (KRaft) · Mailhog · Elasticsearch/Logstash/Kibana · Prometheus/Grafana · Zipkin · Spring Boot Admin

---

## Quick Start

### Prerequisites
- Docker 24+ and Docker Compose v2
- JDK 21+ (for local development only)
- Node 20+ (for local Angular development only)

### 1. Clone and start

```bash
git clone <repo-url>
cd taskmaster

# Start everything
make infra-up      # start infrastructure first
make build         # build all Spring Boot JARs
make up            # start application services
```

Or start everything at once:
```bash
docker compose up -d
```

**Wait ~3 minutes for all services to initialize**, especially Keycloak and Elasticsearch.

### 2. Access the apps

| Service | URL | Default Credentials |
|---|---|---|
| User App | http://localhost:4200 | Created during tenant onboarding |
| Admin App | http://localhost:4201 | `master-admin` / `Admin@123!` (change on first login) |
| API Gateway Swagger | http://localhost:8080/swagger-ui.html | — |
| Keycloak | http://localhost:8180 | `admin` / `admin` |
| Spring Boot Admin | http://localhost:8090/admin | `admin` / `admin` |
| Kafka UI | http://localhost:9091 | — |
| Mailhog | http://localhost:8025 | — |
| Kibana | http://localhost:5601 | — |
| Grafana | http://localhost:3000 | `admin` / `admin` |
| Zipkin | http://localhost:9411 | — |
| Prometheus | http://localhost:9090 | — |

---

## Tenant Onboarding

### Step 1: Login as MASTER_ADMIN
```
URL: http://localhost:4201
Username: master-admin
Password: Admin@123! (change on first login)
```

### Step 2: Create a new tenant via Admin App
1. Navigate to **Tenants** → **Create Tenant**
2. Fill in:
   - Tenant name and domain (e.g., `acme.com`)
   - First TENANT_ADMIN user credentials
3. Submit — this:
   - Creates a `Tenant` row in the database
   - Creates a Keycloak user in the `taskmaster-admin` realm with `tenant_id` attribute
   - Assigns the `TENANT_ADMIN` role

### Step 3: TENANT_ADMIN creates end users
1. Login to admin app as the new TENANT_ADMIN
2. Navigate to **Users** → **Create User**
3. Fill in user details — this:
   - Creates user in `taskmanager_users` DB
   - Creates Keycloak user in `taskmaster-app` realm with `tenant_id` attribute
   - Assigns `USER` role by default

### Step 4: End user logs in
```
URL: http://localhost:4200
Username/Email: <created above>
Password: <set during creation>
```
Or use Google SSO (requires Google OAuth2 credentials — see below).

---

## Configuration

### Environment Variables
Create a `.env` file in the project root (not committed):

```bash
# Google OAuth2 (for SSO login)
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Keycloak service account secret
KEYCLOAK_BACKEND_SECRET=change-in-production

# Firebase Cloud Messaging (for web push notifications)
FCM_SERVICE_ACCOUNT_KEY=/path/to/serviceAccountKey.json
FCM_PROJECT_ID=your-firebase-project-id
```

### Profiles
Each Spring Boot service supports four Spring profiles:
| Profile | Usage |
|---|---|
| `local` | Docker Compose (default) |
| `dev` | Development environment |
| `stage` | Staging environment |
| `prod` | Production environment |

Profile config files are in `backend/config-server/config-files/`.

---

## Development

### Build a single service
```bash
./gradlew :task-service:build
docker compose build task-service
docker compose up -d task-service
```

### Hot-reload debugging
The `docker-compose.override.yml` adds JDWP debug ports:
- user-service: `5005`
- task-service: `5006`
- notification-service: `5007`
- scheduler-service: `5008`

Connect your IDE to `localhost:500X` for remote debugging.

### Angular local development
```bash
cd frontend/user-frontend
npm install
npm start    # http://localhost:4200

cd frontend/admin-frontend
npm install
npm start    # http://localhost:4201
```

---

## Service Ports Reference

| Service | Port | Notes |
|---|---|---|
| API Gateway | 8080 | Main entry point |
| User Service | 8081 | Direct (bypass gateway in dev) |
| Task Service | 8082 | Direct |
| Notification Service | 8083 | Direct |
| Scheduler Service | 8084 | Direct |
| Service Registry (Eureka) | 8761 | `http://localhost:8761/eureka/` |
| Config Server | 8888 | |
| Spring Boot Admin | 8090 | `http://localhost:8090/admin` |
| User Frontend | 4200 | |
| Admin Frontend | 4201 | |

---

## API Documentation

Centralized Swagger UI at the gateway: **http://localhost:8080/swagger-ui.html**

Select a service from the dropdown:
- `user-service` — `/api/v1/tenants`, `/api/v1/users`, `/api/v1/roles`, `/api/v1/permissions`
- `task-service` — `/api/v1/tasks`
- `notification-service` — `/api/v1/notifications`, `/api/v1/notifications/preferences`
- `scheduler-service` — `/api/v1/scheduler/tasks`

GraphQL playground (task-service): **http://localhost:8080/graphiql**

> All REST endpoints follow the `/api/v1/{resource}` versioning convention. Future breaking changes will increment to `/api/v2/`.

---

## Project Structure

```
TaskMaster/
├── backend/
│   ├── api-gateway/              Spring Cloud Gateway
│   ├── user-service/             Users, Tenants, Roles, Permissions
│   ├── task-service/             Task CRUD + GraphQL + Kafka Outbox
│   ├── notification-service/     Email + Push (Kafka consumer)
│   ├── scheduler-service/        Quartz scheduling
│   ├── config-server/            Spring Cloud Config + profile files
│   ├── service-registry/         Eureka Server
│   └── admin-server/             Spring Boot Admin
├── frontend/
│   ├── user-frontend/            Angular 19 — end user app
│   └── admin-frontend/           Angular 19 — admin app
├── infrastructure/
│   ├── keycloak/                 Realm configuration JSONs
│   ├── postgres/                 DB initialization
│   ├── logstash/                 Log pipeline config
│   ├── prometheus/               Scrape config
│   └── grafana/                  Dashboard provisioning
├── docker-compose.yml
├── docker-compose.override.yml   Dev debug ports
├── Makefile                      Convenience commands
├── README.md
└── ARCHITECTURE.md               Detailed architecture guide
```

---

## Roles & Permissions

| Role | Access |
|---|---|
| `MASTER_ADMIN` | All tenants, all operations |
| `TENANT_ADMIN` | Own tenant — manage users, roles, view stats |
| `USER` | Own tasks + assigned tasks within their tenant |
| `READONLY` | View-only within their tenant |

See [ARCHITECTURE.md](./ARCHITECTURE.md) for the complete permissions matrix and security design.

---

## Scaling

To run multiple instances of a service, use Docker Compose `scale`:
```bash
docker compose up -d --scale task-service=3 --scale notification-service=2
```

Services register with Eureka and the API Gateway automatically load-balances across instances. Quartz in the scheduler-service is configured for clustered mode — multiple instances safely share one DB-backed job store.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Kotlin 2.1, Spring Boot 3.4, Spring Security, Spring Cloud |
| API Layer | Spring Cloud Gateway, Spring GraphQL, OpenFeign |
| Auth | Keycloak 26, OAuth2/OIDC, JWT |
| Database | PostgreSQL 17, Flyway migrations, JPA/Hibernate |
| Cache | Redis 7 (`@Cacheable`, idempotency) |
| Messaging | Kafka 3.7 (KRaft), Transactional Outbox Pattern |
| Scheduling | Quartz (clustered, JDBC store) |
| Observability | Micrometer, Zipkin, ELK Stack, Prometheus, Grafana |
| Frontend | Angular 19, Angular Material, NgRx, Angular OIDC, Apollo GraphQL |
| Infrastructure | Docker Compose, Nginx |
