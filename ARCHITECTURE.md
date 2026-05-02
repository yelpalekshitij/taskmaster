# TaskMaster — Architecture Reference

This document provides a deep dive into every architectural component of the TaskMaster platform. Use it to understand how systems interact, how to debug issues, and how to scale or extend the platform.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Multi-Tenancy Design](#2-multi-tenancy-design)
3. [Authentication & Authorization](#3-authentication--authorization)
4. [API Gateway](#4-api-gateway)
5. [Service Discovery & Load Balancing](#5-service-discovery--load-balancing)
6. [Configuration Management](#6-configuration-management)
7. [user-service](#7-user-service)
8. [task-service & Spring GraphQL](#8-task-service--spring-graphql)
9. [Kafka Outbox Pattern](#9-kafka-outbox-pattern)
10. [notification-service](#10-notification-service)
11. [scheduler-service](#11-scheduler-service)
12. [Distributed Logging (ELK)](#12-distributed-logging-elk)
13. [Distributed Tracing (Zipkin)](#13-distributed-tracing-zipkin)
14. [Metrics (Prometheus + Grafana)](#14-metrics-prometheus--grafana)
15. [Redis Caching](#15-redis-caching)
16. [Database Design](#16-database-design)
17. [Security Deep Dive](#17-security-deep-dive)
18. [Frontend Architecture](#18-frontend-architecture)
19. [Scaling & High Availability](#19-scaling--high-availability)
20. [Adding a New Feature](#20-adding-a-new-feature)
21. [Adding a New Tenant](#21-adding-a-new-tenant)
22. [API Reference Index](#22-api-reference-index)

---

## 1. System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         External Clients                                │
│  ┌─────────────────┐              ┌─────────────────────────┐          │
│  │ user-frontend   │              │    admin-frontend       │          │
│  │ Angular 19      │              │    Angular 19           │          │
│  │ port 4200       │              │    port 4201            │          │
│  │ realm: app      │              │    realm: admin         │          │
│  └────────┬────────┘              └──────────┬──────────────┘          │
└───────────┼──────────────────────────────────┼─────────────────────────┘
            │  Bearer JWT                       │ Bearer JWT
            ▼                                  ▼
┌───────────────────────────────────────────────────────────────────────┐
│                  API Gateway (Spring Cloud Gateway)                    │
│  port 8080                                                            │
│  • Validates JWT from both realms (taskmaster-admin + taskmaster-app) │
│  • Injects X-Tenant-Id, X-User-Id headers from JWT claims            │
│  • Routes to microservices via Eureka service discovery (lb://)      │
│  • Aggregates Swagger UI from all downstream services                 │
│  • Propagates W3C TraceContext headers for distributed tracing       │
└──────┬─────────────┬──────────────┬───────────────┬──────────────────┘
       │             │              │               │
       ▼             ▼              ▼               ▼
 user-service  task-service  notification  scheduler-service
 :8081         :8082         -service       :8084
                             :8083
       │             │              ▲               │
       └─────────────┼──────────────┼───────────────┘
                     │   Kafka      │ Feign (REST lb://)
                     ▼   Outbox     │
              [notification-events] │
              [notification-events.DLT]
```

### Component Responsibilities

| Component | Port | Responsibility |
|---|---|---|
| `user-frontend` | 4200 | End-user Angular app. Task management, notifications, profile. OIDC via taskmaster-app realm. Apollo GraphQL client. |
| `admin-frontend` | 4201 | Admin Angular app. Tenant/user management, reports. OIDC via taskmaster-admin realm. REST API client. |
| `api-gateway` | 8080 | Entry point. JWT validation, tenant header extraction, routing, Swagger aggregation. |
| `service-registry` | 8761 | Eureka Server. All services register here for dynamic discovery. |
| `config-server` | 8888 | Spring Cloud Config. Centralized config per service per profile. |
| `user-service` | 8081 | Tenants, users, roles, permissions. Keycloak Admin API integration. |
| `task-service` | 8082 | Task CRUD, Spring GraphQL, Kafka Outbox publisher. |
| `notification-service` | 8083 | Kafka consumer for async notification delivery. Email (Mailhog) + FCM push. |
| `scheduler-service` | 8084 | Quartz-based task scheduling. Triggers status changes and due-date reminders. |
| `admin-server` | 8090 | Spring Boot Admin. Monitors all services, runtime log-level changes, JMX. |

---

## 2. Multi-Tenancy Design

### Strategy: Single Database, Tenant ID Column

Every tenant's data coexists in the same databases. Isolation is enforced at the application layer — every DB query is scoped to `tenant_id`.

```
┌─────────────────────────────────┐
│         Request arrives         │
│  JWT: { tenant_id: "uuid-abc" } │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  TenantHeaderGatewayFilter       │
│  (api-gateway)                  │
│  Extracts tenant_id from JWT    │
│  Sets X-Tenant-Id: uuid-abc     │
└──────────────┬──────────────────┘
               │ HTTP header forwarded
               ▼
┌─────────────────────────────────┐
│  TenantFilter                   │
│  (each microservice)            │
│  Reads X-Tenant-Id header       │
│  → TenantContext.set("uuid-abc")│
└──────────────┬──────────────────┘
               │ ThreadLocal
               ▼
┌─────────────────────────────────┐
│  Repository / Service Layer     │
│  Every query includes:          │
│  WHERE tenant_id = 'uuid-abc'   │
└─────────────────────────────────┘
```

### MASTER_ADMIN Special Case

When `tenant_id` in the JWT is blank/null (only `MASTER_ADMIN` has this), `TenantContext.get()` returns `null`. The `SecurityService.isMasterAdmin()` check bypasses tenant filters, allowing cross-tenant queries.

```kotlin
// SecurityService.kt
fun isTenantOwner(tenantId: UUID): Boolean {
    if (isMasterAdmin()) return true  // bypass for master admin
    val ctxTenantId = TenantContext.get()
    return ctxTenantId != null && ctxTenantId == tenantId.toString()
}
```

### Tenant Isolation Guarantee

All controller endpoints use either:
1. `@PreAuthorize("@sec.isTenantOwner(#tenantId)")` for explicit tenant checks
2. Extract `tenantId` from `TenantContext` and pass to repository — never accepting `tenantId` from the request body

This prevents a `TENANT_ADMIN` from accessing another tenant's data even if they know the UUID.

---

## 3. Authentication & Authorization

### Two Keycloak Realms

```
Keycloak (port 8180)
├── Realm: taskmaster-admin
│   ├── Roles: MASTER_ADMIN, TENANT_ADMIN
│   ├── Client: taskmaster-admin-frontend (public, PKCE)
│   └── Client: taskmaster-backend (confidential, service accounts)
│
└── Realm: taskmaster-app
    ├── Roles: USER, READONLY
    ├── Client: taskmaster-user-frontend (public, PKCE)
    └── Client: taskmaster-backend (confidential, service accounts)
```

**Why two realms?** Complete isolation between admin users and end users. Admin sessions cannot accidentally access end-user resources and vice versa. Enables independent password policies, branding, and IdP configurations per realm.

### JWT Structure

Every JWT contains:
```json
{
  "sub": "keycloak-user-uuid",
  "realm_access": {
    "roles": ["USER"]
  },
  "tenant_id": "acme-tenant-uuid",
  "email": "user@acme.com",
  "preferred_username": "john.doe",
  "exp": 1700000000
}
```

The `tenant_id` claim is populated by a **Keycloak Protocol Mapper** that reads the user's `tenant_id` attribute and adds it to the JWT. This is configured in both realm JSON exports (`taskmaster-app-realm.json`, `taskmaster-admin-realm.json`).

### Dual-Realm JWT Validation (API Gateway)

The gateway validates tokens from BOTH realms using a chained `NimbusReactiveJwtDecoder`:

```kotlin
// SecurityConfig.kt (api-gateway)
fun reactiveAuthenticationManager(): ReactiveAuthenticationManager {
    val adminDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(adminJwkUri).build()
    val appDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(appJwkUri).build()
    
    // Try admin realm first, fall back to app realm
    return ReactiveAuthenticationManager { token ->
        JwtReactiveAuthenticationManager(adminDecoder).authenticate(token)
            .onErrorResume { JwtReactiveAuthenticationManager(appDecoder).authenticate(token) }
    }
}
```

Each microservice is also a resource server and validates the JWT independently (defense in depth).

### Roles & Permissions Matrix

| Permission | MASTER_ADMIN | TENANT_ADMIN | USER | READONLY |
|---|---|---|---|---|
| TASK_CREATE | ✓ | ✓ | ✓ | — |
| TASK_READ | ✓ | ✓ | ✓ | ✓ |
| TASK_UPDATE | ✓ | ✓ | ✓ (own) | — |
| TASK_DELETE | ✓ | ✓ | ✓ (own) | — |
| TASK_ASSIGN | ✓ | ✓ | — | — |
| USER_VIEW | ✓ | ✓ | ✓ | ✓ |
| USER_MANAGE | ✓ | ✓ | — | — |
| TENANT_VIEW | ✓ | ✓ (own) | — | — |
| TENANT_MANAGE | ✓ | — | — | — |
| REPORT_VIEW | ✓ | ✓ | ✓ (own) | ✓ |
| NOTIFICATION_MANAGE | ✓ | ✓ | ✓ (own) | — |
| SCHEDULER_MANAGE | ✓ | ✓ | — | — |

Permissions are stored in the `permissions` table, assigned to roles via `role_permissions`. The `SecurityService` bean reads permissions from the JWT `permissions` claim (populated by a Keycloak role-to-attribute mapper).

### Method Security (SPEL)

All services enable `@EnableMethodSecurity(prePostEnabled = true)`. Custom security SPEL uses the `@Service("sec")` bean:

```kotlin
// Usage in controllers:
@GetMapping("/{tenantId}")
@PreAuthorize("hasRole('MASTER_ADMIN') or @sec.isTenantOwner(#tenantId)")
fun getTenant(@PathVariable tenantId: UUID): ResponseEntity<TenantDto>

@MutationMapping
@PreAuthorize("@sec.hasPermission('TASK_CREATE')")
fun createTask(@Argument input: CreateTaskInput): Task
```

---

## 4. API Gateway

### Request Processing Pipeline

```
Incoming Request
      │
      ▼
RequestLoggingFilter (order -2)
  • Generates request ID
  • Populates MDC: requestId, traceId, spanId, userAgent, method, path
      │
      ▼
Spring Security Filter Chain
  • Validates JWT (dual-realm)
  • Sets SecurityContext with JwtAuthenticationToken
      │
      ▼
TenantHeaderGatewayFilter (order -1)
  • Reads tenant_id from JWT claim
  • Adds X-Tenant-Id header to downstream request
  • Adds X-User-Id header (JWT subject)
      │
      ▼
Route Matching (GatewayRoutesConfig)
  • /api/v1/users/** → lb://user-service
  • /api/v1/tasks/** → lb://task-service
  • /graphql → lb://task-service
  • /api/v1/notifications/** → lb://notification-service
  • /api/v1/scheduler/** → lb://scheduler-service
  • /v3/api-docs/{service} → lb://{service} (Swagger proxy)
      │
      ▼
Eureka Load Balancer
  • Resolves lb://task-service to actual IP:port
  • Round-robin across instances
      │
      ▼
Downstream Microservice
```

### Swagger Aggregation

The gateway proxies API docs from each service:
- `GET /v3/api-docs/user-service` → rewrites to `GET http://user-service:8081/v3/api-docs`
- `GET /v3/api-docs/task-service` → rewrites to `GET http://task-service:8082/v3/api-docs`

The gateway's Springdoc config registers four named groups that the Swagger UI dropdown shows.

**Swagger UI**: http://localhost:8080/swagger-ui.html → select service from dropdown.

---

## 5. Service Discovery & Load Balancing

### Eureka Server

All services register with Eureka at startup:
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://service-registry:8761/eureka/
  instance:
    instance-id: ${spring.application.name}:${random.uuid}
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
```

Each service sends heartbeats every 10 seconds. Instances are deregistered after 30 seconds of missed heartbeats.

### Keycloak master realm SSL fix

The `master` realm has no import JSON — it is always created fresh with `sslRequired=external`. Under Docker Compose, requests arrive from the Docker gateway IP, which Keycloak treats as "external" → 403 HTTPS required on the Keycloak admin console.

The `keycloak-init` service in `docker-compose.yml` runs `kcadm.sh` once after Keycloak becomes healthy to patch the master realm:
```bash
kcadm.sh config credentials --server http://keycloak:8180 --realm master --user admin --password admin
kcadm.sh update realms/master -s sslRequired=NONE
```

The imported app/admin realms (`taskmaster-app-realm.json`, `taskmaster-admin-realm.json`) already have `sslRequired: none` set directly in the JSON.

---

### Security — Open Registry (Local Dev)

`service-registry` ships with a `SecurityConfig` that permits all requests and disables CSRF:

```kotlin
// backend/service-registry/src/main/kotlin/com/taskmaster/registry/config/SecurityConfig.kt
http.csrf { it.disable() }
    .authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
```

**Why open?** Spring Cloud Netflix 2024.0.x strips URL-embedded credentials (`http://user:pass@host/eureka/`) before sending the HTTP request — the Eureka client never transmits Basic Auth headers even when credentials are present in `defaultZone`. Requiring authentication on the registry therefore blocks all client registrations. Opening the registry is the standard practice for Docker Compose local development; production deployments should sit behind a private network or VPN instead.

### Client-Side Load Balancing (Feign + Spring Cloud LoadBalancer)

When a service makes a Feign call:
```kotlin
@FeignClient(name = "notification-service")
interface NotificationClient { ... }
```

Spring Cloud LoadBalancer resolves `notification-service` to a live instance from Eureka registry, using round-robin by default.

### Running Multiple Instances

```bash
docker compose up -d --scale task-service=3
```

All 3 instances register in Eureka. The gateway and Feign clients automatically distribute load across them. No configuration changes needed.

---

## 6. Configuration Management

### Spring Cloud Config Server

All service configurations are stored centrally in `backend/config-server/config-files/`. Services load config at startup via:
```yaml
spring:
  config:
    import: "optional:configserver:http://config-server:8888"
```

### Configuration Resolution Order

Spring Cloud Config resolves config by overlaying files in order (lowest to highest priority):
1. `application.yml` — shared defaults for ALL services
2. `application-{profile}.yml` — shared profile overrides
3. `{service-name}.yml` — service-specific defaults
4. `{service-name}-{profile}.yml` — service + profile specifics

For example, for `task-service` on `local` profile:
```
application.yml
  ↓ overlaid by
application-local.yml
  ↓ overlaid by
task-service.yml
  ↓ overlaid by
task-service-local.yml (highest priority)
```

### Adding a New Configuration

1. Edit the appropriate file in `backend/config-server/config-files/`
2. Either restart the service, or call `/actuator/refresh` (Spring Cloud Config Bus not configured yet)

### Deploying to dev / stage / prod

Set `SPRING_PROFILES_ACTIVE` to the target profile on every service container. Config resolution picks up the matching `application-{profile}.yml` from the config-server automatically.

| Profile | `SPRING_PROFILES_ACTIVE` | Config file loaded |
|---|---|---|
| Local Docker Compose | `local` | `application-local.yml` |
| Dev environment | `dev` | `application-dev.yml` |
| Staging | `stage` | `application-stage.yml` |
| Production | `prod` | `application-prod.yml` |

**Key things that must change per environment** (all marked with `# CHANGE` comments in the config files):

| Concern | Local | Dev | Stage | Prod |
|---|---|---|---|---|
| DB | Docker postgres container | Managed RDS | Managed RDS | Managed RDS (Aurora) |
| Redis | Docker Redis container | Managed ElastiCache | Managed ElastiCache | Managed ElastiCache + TLS |
| Kafka | Docker Kafka PLAINTEXT | Managed MSK | Managed MSK | Managed MSK + SASL_SSL |
| Keycloak | `start-dev` + `dev-mem` + `sslRequired: none` | Dedicated Keycloak + postgres | Dedicated Keycloak + postgres | `start` + postgres + HTTPS + `sslRequired: all` |
| Secrets | Hardcoded in config files | Env vars / `.env` file | Env vars injected by CI | AWS SSM / Vault — never in files |
| Tracing | 100% | 100% | 50% | 10% |
| Actuator exposure | All endpoints | All endpoints | health, info, prometheus | health, prometheus |
| CORS origins | localhost:4200/4201 | Dev frontend URL | Stage frontend URL | Production frontend URL |
| Quartz clustering | `isClustered: false` | `false` (1 instance) | `true` if scaled | `true` |
| Elasticsearch security | `xpack.security.enabled=false` | enabled | enabled | enabled + TLS |

---

## 7. user-service

### Responsibilities
- Tenant CRUD with activation/deactivation
- User CRUD with Keycloak provisioning
- Role and permission management
- Tenant statistics (user counts)

### Keycloak Admin Integration

When creating a tenant or user, the service calls the Keycloak Admin REST API to create the user in the appropriate realm and assign the correct role.

```kotlin
// KeycloakAdminService.kt
fun createTenantAdmin(tenantId, email, username, ...): String {
    val kc = Keycloak.getInstance(serverUrl, "master", adminUsername, adminPassword, "admin-cli")
    
    val user = UserRepresentation().apply {
        this.username = username
        attributes = mapOf("tenant_id" to listOf(tenantId.toString()))
    }
    val response = kc.realm("taskmaster-admin").users().create(user)
    val userId = response.location.path.substringAfterLast("/")
    
    val role = kc.realm("taskmaster-admin").roles().get("TENANT_ADMIN").toRepresentation()
    kc.realm("taskmaster-admin").users().get(userId).roles().realmLevel().add(listOf(role))
    return userId
}
```

### Caching Strategy

| Cache Name | TTL | Key | Evicted When |
|---|---|---|---|
| `tenants` | 30 min | tenant UUID | Tenant updated/deactivated |
| `roles` | 60 min | `tenant:{tenantId}` | Role created/updated |
| `permissions` | 24 hours | default | Never (static seeded data) |
| `users` | 10 min | user UUID | User updated |

### Pagination & Filtering

All admin list endpoints use JPA `Specification` for dynamic filtering:
```kotlin
// TenantController.kt
GET /api/v1/tenants?name=acme&active=true&page=0&size=20&sort=name,asc
```

Filtering is composed via `TenantSpecification.buildSpec(name, active, domain)` which chains `Specification` predicates using `.and()`.

---

## 8. task-service & Spring GraphQL

### Two API Layers

The task-service exposes both:
- **GraphQL** (`/graphql`) — used by `user-frontend`, supports queries, mutations, subscriptions
- **REST** (`/api/v1/tasks/**`) — used by admin-frontend and inter-service calls

### GraphQL Schema Summary

```graphql
type Query {
  tasks(filter: TaskFilter, page: Int, size: Int): TaskPage!  # tenant-scoped
  task(id: ID!): Task
  myTasks(page: Int, size: Int): TaskPage!                    # assigned to current user
}

type Mutation {
  createTask(input: CreateTaskInput!): Task!
  updateTask(id: ID!, input: UpdateTaskInput!): Task!
  deleteTask(id: ID!): Boolean!
  updateTaskStatus(id: ID!, status: TaskStatus!): Task!
  assignTask(id: ID!, userId: ID!): Task!
  addComment(taskId: ID!, content: String!): TaskComment!
}
```

Resolver methods are in `TaskResolver.kt` and `TaskCommentResolver.kt`, annotated with `@QueryMapping`, `@MutationMapping`, `@SchemaMapping`.

### GraphQL Error Handling

`GraphQLConfig.kt` implements `DataFetcherExceptionResolverAdapter`:
- `NoSuchElementException` → `NOT_FOUND` GraphQL error
- `AccessDeniedException` → `FORBIDDEN` GraphQL error
- Other exceptions → `INTERNAL_ERROR`

### Task Status Flow

```
  ┌─────────┐     assign/create      ┌────────────┐
  │  TODO   │ ──────────────────────▶│ IN_PROGRESS│
  └────┬────┘                        └──────┬─────┘
       │                                    │
       │ schedule task                      │ on hold
       ▼                                    ▼
  ┌─────────────┐                    ┌──────────┐
  │  SCHEDULED  │                    │  ON_HOLD │
  └─────────────┘                    └──────────┘
                                           │
                              complete     ▼
                         ┌───────────────────────┐
                         │          DONE          │
                         └───────────────────────┘
```

Status changes are recorded in `task_history` table (via `TaskHistoryService`).

---

## 9. Kafka Outbox Pattern

### Problem It Solves

When a task is assigned, we need to:
1. Update the task in the database (**must succeed**)
2. Send a notification to the assignee (**can fail/retry**)

Without the Outbox pattern, a direct notification call could fail and the task update would already be committed — inconsistent state.

### The Pattern

```
TaskService.assignTask()
│
├── BEGIN TRANSACTION
│   ├── UPDATE tasks SET assigned_to = ? WHERE id = ?
│   └── INSERT INTO outbox_events (event_type='TASK_ASSIGNED', payload={...}, published_at=NULL)
└── COMMIT TRANSACTION
     ↑ atomic — both succeed or both fail

OutboxRelay (@Scheduled every 500ms)
│
├── SELECT * FROM outbox_events WHERE published_at IS NULL ORDER BY created_at LIMIT 50
├── FOR EACH event:
│   ├── kafkaTemplate.send("notification-events", event.aggregateId, event.payload).get()
│   ├── UPDATE outbox_events SET published_at = NOW()
│   └── on failure: UPDATE publish_attempts = publish_attempts + 1, log error
└── Retry on next poll
```

### Kafka Topic Design

| Topic | Partitions | Partition Key | Retention |
|---|---|---|---|
| `notification-events` | 6 | `aggregate_id` (task UUID) | 7 days |
| `notification-events.DLT` | 1 | — | 30 days |

**6 partitions** — allows 6 parallel notification-service consumers. Keyed by `task_id` ensures events for the same task are processed in order.

### Idempotency Key

The outbox row has a `UNIQUE` constraint on `idempotency_key = "${taskId}:${eventType}:${epochMinute}"`. If the same event type is triggered twice within the same minute for the same task, the second insert silently fails (duplicate key) — preventing double notifications.

### Consumer Idempotency (notification-service)

The consumer performs an additional check:
```kotlin
val alreadyProcessed = redisTemplate.opsForValue()
    .setIfAbsent("idempotency::$eventId", "processed", Duration.ofHours(24))
if (alreadyProcessed == false) {
    log.warn("Duplicate event ignored: eventId={}", eventId)
    return
}
```

This guards against Kafka at-least-once delivery delivering the same message twice.

### Dead Letter Topic

After 3 retry attempts (with 1-second backoff), Spring Kafka's `DefaultErrorHandler` with `DeadLetterPublishingRecoverer` sends the failed message to `notification-events.DLT`. The `DltConsumer` logs the failure for manual investigation.

---

## 10. notification-service

### Architecture

```
Kafka Consumer Thread
       │
       ▼
NotificationEventConsumer.consume()
       │
       ├── 1. Idempotency check (Redis SETNX)
       ├── 2. Load notification preferences for assignedTo user
       ├── 3. Persist Notification record in DB
       ├── 4. emailEnabled? → EmailService.sendNotification()
       │                              │
       │                              └─▶ JavaMailSender → Mailhog SMTP :1025
       └── 5. pushEnabled + fcmToken? → FcmService.sendPush()
                                              │
                                              └─▶ FCM v1 REST API
```

### Email Templates (Thymeleaf)

Templates in `resources/templates/`:
- `notification.html` — generic notification email

Variables: `subject`, `message`, `eventType` → rendered to HTML before sending.

### FCM Web Push

`FcmService.kt` sends push notifications via the FCM v1 HTTP API. The FCM token (device registration token) is stored in `notification_preferences.fcm_token` and updated when the user's browser registers the service worker.

**To enable FCM:**
1. Create a Firebase project at https://console.firebase.google.com
2. Download the service account JSON key
3. Set `FCM_SERVICE_ACCOUNT_KEY=/path/to/key.json` in `.env`
4. Set `FCM_PROJECT_ID=your-firebase-project-id`

The Angular `user-frontend` includes a service worker (`firebase-messaging-sw.js`) that handles background push messages.

### Preference Management

Users control notification delivery via:
```
PUT /api/v1/notifications/preferences
{
  "emailEnabled": true,
  "pushEnabled": true,
  "fcmToken": "browser-registration-token",
  "email": "user@example.com"
}
```

Preferences are auto-created with defaults on first access.

---

## 11. scheduler-service

### Quartz Configuration

Quartz uses Spring Boot's `LocalDataSourceJobStore` with the `taskmanager_scheduler` PostgreSQL database. The job store class is wired automatically by Spring's `SchedulerFactoryBean.setDataSource()` — do **not** set `org.quartz.jobStore.class` explicitly (that bypasses Spring's DataSource injection and causes startup failures).

```yaml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: always   # creates QRTZ_* tables on first start
    properties:
      org.quartz.scheduler.instanceName: TaskMasterScheduler
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.jobStore.driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
      org.quartz.jobStore.tablePrefix: QRTZ_
      org.quartz.jobStore.isClustered: false   # set true when scaling horizontally
      org.quartz.threadPool.threadCount: 5
```

Quartz creates its own `QRTZ_*` tables in `taskmanager_scheduler`. When `isClustered: true`, multiple instances share the job store using optimistic row-level locking — each job fires on exactly one instance.

### Job Types

| Job | Trigger | Effect |
|---|---|---|
| `TaskSchedulerJob` | One-shot at `scheduled_at` | Calls task-service to set status → `SCHEDULED` |
| `TaskDueReminderJob` | One-shot 1 hour before `due_date` | Calls notification-service to send TASK_DUE event |

### Scheduling a Task

```http
POST /api/v1/scheduler/tasks/{taskId}/schedule
{
  "scheduledAt": "2025-06-15T09:00:00Z"
}
```

This creates a Quartz `JobDetail` with the task ID in the data map and a one-time `SimpleTrigger`.

### Inter-Service Auth (Scheduler)

Scheduler calls notification-service and task-service without a user JWT. It uses the **Keycloak Client Credentials flow** to obtain a machine-to-machine token:

```kotlin
// FeignSecurityConfig.kt
@Bean
fun clientCredentialsInterceptor(): RequestInterceptor {
    return RequestInterceptor { template ->
        val token = keycloakService.getServiceAccountToken()
        template.header("Authorization", "Bearer $token")
    }
}
```

---

## 12. Distributed Logging (ELK)

### Log Pipeline

```
Spring Boot Service
    │ logstash-logback-encoder JSON
    │ (async TCP appender)
    ▼
Logstash :5000/TCP
    │ Parse JSON, add @timestamp, service_name
    ▼
Elasticsearch :9200
    │ Index: taskmaster-logs-{YYYY.MM.dd}
    ▼
Kibana :5601
    │ Index pattern: taskmaster-logs-*
    ▼
Dashboard/Search
```

### Log Format

Every log line is a JSON object with:
```json
{
  "@timestamp": "2025-01-15T10:30:00.123Z",
  "level": "INFO",
  "service_name": "task-service",
  "service_version": "1.0.0",
  "logger_name": "com.taskmaster.task.TaskService",
  "message": "Task created: id=uuid",
  "thread_name": "http-nio-8082-exec-1",
  "mdc": {
    "traceId": "abc123def456",
    "spanId": "789abc",
    "userId": "user-keycloak-uuid",
    "tenantId": "tenant-uuid",
    "requestId": "req-uuid",
    "userAgent": "Mozilla/5.0 ...",
    "httpMethod": "POST",
    "httpPath": "/graphql"
  },
  "stack_trace": "..."  // only on ERROR
}
```

### MDC Population (LoggingMdcFilter)

`LoggingMdcFilter.kt` (in each service) runs as `OncePerRequestFilter` and populates MDC:
1. `traceId` / `spanId` — from Micrometer `Tracer.currentSpan().context()`
2. `userId` — from `JwtAuthenticationToken.token.subject`
3. `tenantId` — from `TenantContext.get()`
4. `requestId` — from `X-Request-ID` header or new UUID
5. `userAgent` — from HTTP header

### Searching in Kibana

1. Open http://localhost:5601
2. Navigate to **Discover**
3. Select index pattern `taskmaster-logs-*`
4. Search examples:
   - `mdc.traceId: "abc123"` — all logs for one distributed request
   - `level: ERROR and service_name: task-service` — errors in task service
   - `mdc.userId: "uuid" and mdc.httpMethod: POST` — all POST requests by a user

---

## 13. Distributed Tracing (Zipkin)

### How Trace Propagation Works

```
User Browser
    │ HTTP POST /graphql
    │ W3C TraceContext headers NOT present (first hop)
    ▼
API Gateway
    │ Micrometer creates root span: traceId=abc123, spanId=span1
    │ Forwards headers: traceparent: 00-abc123-span1-01
    ▼
task-service
    │ Micrometer picks up traceId=abc123, creates child span: spanId=span2
    │ Forwards headers on Feign call to notification-service
    ▼
notification-service (via Feign)
    │ Micrometer: traceId=abc123, spanId=span3

Zipkin collects all spans → reconstruct full trace by traceId
```

### Searching in Zipkin

1. Open http://localhost:9411
2. Enter `traceId` in search box
3. See full request waterfall: gateway → task-service → notification-service

### Configuration

All services:
```yaml
management.tracing.sampling.probability: 1.0  # local (100%)
# production: 0.1 (10%)
```

Dependencies in each service:
```gradle
implementation("io.micrometer:micrometer-tracing-bridge-otel")
implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
```

---

## 14. Metrics (Prometheus + Grafana)

### What's Collected

Spring Boot Actuator automatically exposes at `/actuator/prometheus`:
- JVM metrics: heap, GC, threads
- HTTP request rate, latency, error rate (by endpoint)
- Datasource pool metrics (active connections, pending)
- Cache metrics (hit rate, miss rate)
- Kafka consumer lag

Custom metrics (examples to add):
```kotlin
// In TaskService:
val tasksCreatedCounter = Counter.builder("tasks.created")
    .tag("tenantId", tenantId.toString())
    .register(meterRegistry)
tasksCreatedCounter.increment()
```

### Prometheus Scraping

Prometheus scrapes `/actuator/prometheus` every 15 seconds from all services (see `infrastructure/prometheus/prometheus.yml`).

### Grafana Dashboards

Access: http://localhost:3000 (`admin`/`admin`)

Pre-configured datasource: Prometheus at `http://prometheus:9090`.

**Useful dashboard queries:**
```
# HTTP error rate per service
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)

# 99th percentile response time
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))

# Active DB connections
hikaricp_connections_active{application="task-service"}
```

---

## 15. Redis Caching

### Cache Topology

```
Application → @Cacheable("roles") lookup
                    │
                    ▼
              Redis HASH key: "roles::tenant:{uuid}"
                    │
                    ├── Cache HIT → return cached value
                    └── Cache MISS → query DB → store in Redis → return
```

### Cache Regions and TTLs

| Cache | TTL | Eviction Trigger |
|---|---|---|
| `tenants` | 30 min | `@CacheEvict` on update/deactivate |
| `roles` | 60 min | `@CacheEvict` on role create/update |
| `permissions` | 24 hours | Static data, evict on application restart |
| `users` | 10 min | `@CacheEvict` on user update |
| `tasks` | 5 min | `@CacheEvict` on task update |

### Idempotency Cache (notification-service)

The notification consumer uses Redis with `SETNX` (Set if Not eXists):
```kotlin
redisTemplate.opsForValue()
    .setIfAbsent("idempotency::$eventId", "processed", Duration.ofHours(24))
```

This guarantees at-most-once notification delivery even with Kafka at-least-once semantics.

### Direct Redis Access

Services have `RedisTemplate<String, String>` injected for custom operations beyond `@Cacheable`.

---

## 16. Database Design

### Database-per-Service

Each microservice has its own PostgreSQL database, preventing tight coupling:

| Service | Database | Why Separate |
|---|---|---|
| user-service | `taskmanager_users` | User/tenant data — high access, caching |
| task-service | `taskmanager_tasks` | Large table — task history, comments |
| notification-service | `taskmanager_notifications` | Append-heavy, high write rate |
| scheduler-service | `taskmanager_scheduler` | Quartz tables + schedule records |

### Flyway Migrations

All migrations in `src/main/resources/db/migration/`:
```
V1__initial_schema.sql    ← tables, types, indexes
V2__seed_roles_permissions.sql  ← seeded static data (user-service only)
```

Flyway runs at service startup. The `baseline-on-migrate: true` setting handles pre-existing databases safely.

### Adding a Migration

Create `V3__add_column_xyz.sql` in the migration folder. It will be picked up on next service restart.

```sql
-- V3__add_task_priority.sql
ALTER TABLE tasks ADD COLUMN priority_score INTEGER DEFAULT 0;
```

### Index Strategy

All `tenant_id` columns are indexed. Composite indexes on `(user_id, read)` for notification queries. Partial index on outbox events `WHERE published_at IS NULL` for efficient relay polling.

---

## 17. Security Deep Dive

### Defense in Depth

1. **Gateway layer**: JWT signature validated before any request reaches services
2. **Service layer**: Each service independently validates the JWT (resource server)
3. **Controller layer**: `@PreAuthorize` annotations on every endpoint
4. **Repository layer**: All queries include tenant ID from `TenantContext`
5. **Feign layer**: JWT forwarded on all service-to-service calls

### Cross-Tenant Data Access Prevention

```kotlin
// TaskController.kt
@GetMapping("/{taskId}")
@PreAuthorize("hasRole('MASTER_ADMIN') or @sec.isTenantOwner(#tenantId)")
fun getTask(@PathVariable taskId: UUID): ResponseEntity<TaskDto> {
    val tenantId = UUID.fromString(TenantContext.getRequired())
    // tenantId comes from JWT via header, NOT from request path
    return ResponseEntity.ok(taskService.findById(taskId, tenantId))
}
```

The `tenantId` is **never** accepted from the request body or path. It's always sourced from the JWT → gateway header → `TenantContext`.

### CSRF Protection

Disabled (stateless REST/GraphQL API using JWT tokens — no cookies involved).

### CORS Configuration

Configured per service to allow only:
- `http://localhost:4200` (user-frontend)
- `http://localhost:4201` (admin-frontend)

All other origins are rejected.

### Google SSO

Configured as Identity Provider in both Keycloak realms. When a user logs in with Google:
1. Keycloak authenticates with Google OAuth2
2. Keycloak imports the user if not present (`IMPORT` sync mode)
3. User must have `tenant_id` attribute set (admin must do this post-provisioning for Google users)

---

## 18. Frontend Architecture

### user-frontend

```
frontend/user-frontend/
├── app.config.ts          ← OIDC (taskmaster-app realm) + Apollo + NgRx + HttpClient
├── app.routes.ts          ← Lazy-loaded, all guarded by autoLoginPartialRoutesGuard
├── core/
│   ├── auth/              ← OIDC callback handling
│   ├── interceptors/      ← Attaches Bearer token to all HTTP/GraphQL requests
│   ├── services/          ← task.service.ts (Apollo), notification.service.ts (REST)
│   └── models/            ← TypeScript interfaces
├── features/
│   ├── dashboard/         ← Task stats, quick actions
│   ├── tasks/             ← task-list, kanban-board, task-form-dialog
│   ├── notifications/     ← notification list, mark-as-read
│   └── profile/           ← user info, notification preferences
└── shared/
    └── components/        ← status-badge, etc.
```

**OIDC Flow (angular-auth-oidc-client):**
1. App loads → `autoLoginPartialRoutesGuard` checks for valid token
2. No token → redirect to Keycloak (`taskmaster-app` realm) login page
3. User logs in → Keycloak redirects to `/callback?code=...`
4. `CallbackComponent` handles the code exchange
5. Token stored in memory + silent renew via refresh token

**GraphQL Client (Apollo Angular):**
```typescript
// task.service.ts
getTasks(filter?: TaskFilter, page = 0, size = 20): Observable<ApolloQueryResult<{tasks: TaskPage}>> {
  return this.apollo.watchQuery({
    query: GET_TASKS,
    variables: { filter, page, size }
  }).valueChanges;
}
```

### admin-frontend

Same structure but:
- OIDC points to `taskmaster-admin` realm
- No Apollo — uses `HttpClient` REST services
- Role guards: `masterAdminGuard` and `tenantAdminGuard` restrict route access
- Deep purple Material theme (visual differentiation)

---

## 19. Scaling & High Availability

### Stateless Services

All backend services are fully stateless (no in-process session, all state in DB/Redis). Scale by adding instances:

```bash
docker compose up -d --scale task-service=5
```

### Redis for Shared State

Caches and idempotency keys are stored in Redis, shared across all instances of the same service. No stale cache issues with multiple replicas.

### Quartz Clustering

The scheduler-service uses Quartz in clustered mode with PostgreSQL as the job store. All scheduler instances share the same jobs. Quartz uses row-level locking to ensure a job fires on exactly one instance.

### Kafka Consumer Groups

All notification-service instances share the same `group-id: notification-service`. Kafka distributes partitions across instances. With 6 partitions in `notification-events`, up to 6 parallel consumers process notifications.

### Database Scaling

- **Read replicas**: Add `spring.datasource.replica.url` and route read queries there
- **Connection pooling**: HikariCP (default) pools connections; tune `maximum-pool-size`
- **Postgres partitioning**: The `tasks` table can be partitioned by `tenant_id` at large scale

### Adding a New Microservice

1. Create module in `settings.gradle.kts`
2. Use `user-service` as a template for common patterns:
   - `TenantContext`, `TenantFilter`, `LoggingMdcFilter`, `SecurityService`
   - `SecurityConfig` with dual-realm JWT
   - Logback JSON config
3. Register Feign client in other services that need it
4. Add to `docker-compose.yml`
5. Add routes in `api-gateway/GatewayRoutesConfig.kt`
6. Add config files in `backend/config-server/config-files/`
7. Add Prometheus scrape config
8. Add Swagger URL to gateway's Springdoc config

---

## 20. Adding a New Feature

### Backend (example: task labels/tags)

1. Add migration `V3__add_task_labels.sql`
2. Add/update entity fields
3. Add repository query if needed
4. Update `TaskService` business logic
5. Update GraphQL schema (`schema.graphqls`) if user-facing
6. Update REST controller if admin-facing
7. Write unit tests for service
8. Write integration test for controller

### Frontend (example: task labels)

1. Update `task.model.ts` with label types
2. Update GraphQL queries in `task.service.ts`
3. Add label chips to `task-list.component.ts` and `task-form-dialog.component.ts`
4. Write component test

---

## 21. Adding a New Tenant

### Via API (MASTER_ADMIN)

```http
POST http://localhost:8080/api/v1/tenants
Authorization: Bearer <MASTER_ADMIN_JWT>
Content-Type: application/json

{
  "name": "Acme Corp",
  "domain": "acme.com",
  "adminEmail": "admin@acme.com",
  "adminUsername": "acme-admin",
  "adminFirstName": "Jane",
  "adminLastName": "Doe",
  "adminPassword": "SecurePass@123"
}
```

**What happens internally:**
1. Creates `Tenant` row in `taskmanager_users`
2. Calls Keycloak Admin API → creates user in `taskmaster-admin` realm with `tenant_id` attribute
3. Assigns `TENANT_ADMIN` role to the Keycloak user
4. Returns the tenant DTO

### TENANT_ADMIN Provisions End Users

```http
POST http://localhost:8080/api/v1/users
Authorization: Bearer <TENANT_ADMIN_JWT>
X-Tenant-Id: <tenant-uuid>  (injected by gateway, not needed manually)
Content-Type: application/json

{
  "email": "john@acme.com",
  "username": "john.doe",
  "firstName": "John",
  "lastName": "Doe",
  "password": "UserPass@123",
  "roleIds": ["00000000-0000-0000-0000-000000000003"]  // USER role UUID
}
```

**What happens internally:**
1. Creates `User` row in `taskmanager_users`
2. Calls Keycloak Admin API → creates user in `taskmaster-app` realm with `tenant_id` attribute
3. Assigns `USER` role to the Keycloak user
4. Returns the user DTO

---

## 22. API Reference Index

Full interactive docs at: **http://localhost:8080/swagger-ui.html**

GraphQL playground: **http://localhost:8080/graphiql**

### REST Endpoints Summary

#### user-service (`/api/v1/users`, `/api/v1/tenants`, `/api/v1/roles`)

| Method | Path | Role Required | Description |
|---|---|---|---|
| GET | /api/v1/tenants | MASTER_ADMIN | List all tenants (paginated) |
| POST | /api/v1/tenants | MASTER_ADMIN | Create tenant + first admin |
| GET | /api/v1/tenants/{id} | MASTER_ADMIN or own tenant | Get tenant |
| PATCH | /api/v1/tenants/{id} | MASTER_ADMIN | Update tenant |
| DELETE | /api/v1/tenants/{id} | MASTER_ADMIN | Deactivate tenant |
| GET | /api/v1/tenants/{id}/stats | MASTER_ADMIN or own tenant | Tenant user stats |
| GET | /api/v1/users | TENANT_ADMIN+ | List users in current tenant |
| POST | /api/v1/users | TENANT_ADMIN+ | Create user |
| GET | /api/v1/users/me | Any | Get current user |
| PATCH | /api/v1/users/{id} | TENANT_ADMIN or own user | Update user |
| GET | /api/v1/roles | TENANT_ADMIN+ | List roles for tenant |
| GET | /api/v1/roles/permissions | TENANT_ADMIN+ | List all permissions |
| POST | /api/v1/roles | TENANT_ADMIN+ | Create custom role |

#### task-service REST (`/api/v1/tasks`)

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | /api/v1/tasks | TASK_READ | List tasks (paginated, filterable) |
| POST | /api/v1/tasks | TASK_CREATE | Create task |
| PATCH | /api/v1/tasks/{id}/status | TASK_UPDATE | Update status |
| PATCH | /api/v1/tasks/{id}/assign | TASK_ASSIGN | Assign to user |
| DELETE | /api/v1/tasks/{id} | TASK_DELETE | Delete task |

#### notification-service (`/api/v1/notifications`)

| Method | Path | Description |
|---|---|---|
| GET | /api/v1/notifications | List notifications (paginated) |
| PATCH | /api/v1/notifications/{id}/read | Mark one as read |
| PATCH | /api/v1/notifications/read-all | Mark all as read |
| GET | /api/v1/notifications/preferences | Get notification preferences |
| PUT | /api/v1/notifications/preferences | Update preferences + FCM token |

#### scheduler-service (`/api/v1/scheduler`)

| Method | Path | Permission | Description |
|---|---|---|---|
| POST | /api/v1/scheduler/tasks/{id}/schedule | SCHEDULER_MANAGE | Schedule a task |
| DELETE | /api/v1/scheduler/tasks/{id} | SCHEDULER_MANAGE | Cancel schedule |
| GET | /api/v1/scheduler/tasks | SCHEDULER_MANAGE | List active schedules |

### GraphQL Operations

See full schema at: `backend/task-service/src/main/resources/graphql/schema.graphqls`

Or query the introspection endpoint: `POST http://localhost:8080/graphql` with `{ "__schema" { ... } }`
