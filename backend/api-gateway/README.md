# api-gateway

Spring Cloud Gateway (reactive) — the single entry point for all client traffic. Validates JWTs, injects tenant context headers, routes to downstream services via Eureka service discovery, and aggregates Swagger UI.

**Port:** `8080`

---

## Responsibilities

- JWT validation against both Keycloak realms (`taskmaster-admin`, `taskmaster-app`)
- Extract `tenant_id` and `sub` (user ID) claims → inject as `X-Tenant-Id` and `X-User-Id` headers
- Route to downstream services with Eureka `lb://` load balancing
- Aggregate Swagger UI: `http://localhost:8080/swagger-ui.html` (select service from dropdown)
- CORS handling for Angular apps on ports 4200 and 4201

---

## Request Pipeline

```
Client Request
    │
    ▼
ReactiveSecurityFilterChain
    │  JWT decoded + validated (try admin realm → try app realm)
    │
    ▼
TenantHeaderGatewayFilter (Order -1)
    │  Reads tenant_id from JWT claims
    │  Adds X-Tenant-Id: {tenantId}
    │  Adds X-User-Id: {userId}
    │
    ▼
Spring Cloud Gateway Route Matching
    │  lb://user-service, lb://task-service, ...
    │
    ▼
Eureka Load Balancer → Downstream Service
```

---

## Routes

| Path Pattern | Downstream Service | Notes |
|---|---|---|
| `/api/v1/users/**` | `lb://user-service` | |
| `/api/v1/tenants/**` | `lb://user-service` | |
| `/api/v1/roles/**` | `lb://user-service` | |
| `/api/v1/tasks/**` | `lb://task-service` | |
| `/graphql` | `lb://task-service` | GraphQL endpoint |
| `/graphiql` | `lb://task-service` | GraphQL playground |
| `/api/v1/notifications/**` | `lb://notification-service` | |
| `/api/v1/scheduler/**` | `lb://scheduler-service` | |
| `/v3/api-docs/user-service` | `lb://user-service/v3/api-docs` | Swagger aggregation |
| `/v3/api-docs/task-service` | `lb://task-service/v3/api-docs` | Swagger aggregation |
| `/v3/api-docs/notification-service` | `lb://notification-service/v3/api-docs` | Swagger aggregation |
| `/v3/api-docs/scheduler-service` | `lb://scheduler-service/v3/api-docs` | Swagger aggregation |

---

## JWT Validation (Dual Realm)

The gateway validates tokens from two Keycloak realms without knowing the issuer upfront:

```kotlin
// Try admin realm first, fall back to app realm
NimbusReactiveJwtDecoder.withJwkSetUri(adminJwkSetUri).build()
    .decode(token)
    .onErrorResume { NimbusReactiveJwtDecoder.withJwkSetUri(appJwkSetUri).build().decode(token) }
```

This avoids any JWKS fetch at startup — decoders are constructed lazily.

---

## Swagger UI

Access: `http://localhost:8080/swagger-ui.html`

Springdoc on the gateway aggregates API specs from all four services. Select a service from the dropdown to view its endpoints and schemas.

Direct API docs URLs:
- `http://localhost:8080/v3/api-docs/user-service`
- `http://localhost:8080/v3/api-docs/task-service`
- `http://localhost:8080/v3/api-docs/notification-service`
- `http://localhost:8080/v3/api-docs/scheduler-service`

---

## Security — Public Paths

The following paths do not require authentication:

- `/swagger-ui.html`, `/swagger-ui/**`, `/webjars/**`
- `/v3/api-docs/**`
- `/actuator/health`
- `/graphiql` (playground only, queries still require auth)

---

## Configuration

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false          # manual routes only
keycloak:
  admin-realm:
    issuer-uri: http://keycloak:8180/realms/taskmaster-admin
  app-realm:
    issuer-uri: http://keycloak:8180/realms/taskmaster-app
```

---

## Running Locally

```bash
docker compose up -d api-gateway

./gradlew :api-gateway:build
docker compose build api-gateway
docker compose up -d --no-deps api-gateway
```

Note: api-gateway depends on service-registry (Eureka) and config-server. Start infra first:
```bash
make infra-up
```

---

## Tests

```bash
./gradlew :api-gateway:test
```

Tests cover: route configuration, `TenantHeaderGatewayFilter` header injection, JWT validation chain.
