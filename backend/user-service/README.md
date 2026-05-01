# user-service

Manages tenants, users, roles, and permissions. Integrates with Keycloak Admin API to create users and assign roles across two realms.

**Port:** `8081`

---

## Responsibilities

- Tenant CRUD (MASTER_ADMIN only for create/list; tenant-scoped for read/update)
- User CRUD within a tenant (TENANT_ADMIN manages users; USER views own profile)
- Role and permission management (system roles + per-tenant custom roles)
- Keycloak user provisioning via Admin API on tenant/user create

---

## API Endpoints

| Method | Path | Role Required | Description |
|---|---|---|---|
| POST | `/api/v1/tenants` | MASTER_ADMIN | Create a new tenant + Keycloak admin user |
| GET | `/api/v1/tenants` | MASTER_ADMIN | List all tenants (paginated) |
| GET | `/api/v1/tenants/{id}` | MASTER_ADMIN or tenant owner | Get tenant details |
| PUT | `/api/v1/tenants/{id}` | MASTER_ADMIN or tenant owner | Update tenant |
| DELETE | `/api/v1/tenants/{id}` | MASTER_ADMIN | Delete tenant |
| GET | `/api/v1/tenants/{id}/stats` | MASTER_ADMIN or tenant owner | User count, active tasks |
| POST | `/api/v1/users` | TENANT_ADMIN | Create a user in current tenant |
| GET | `/api/v1/users` | TENANT_ADMIN | List users in current tenant (paginated) |
| GET | `/api/v1/users/{id}` | TENANT_ADMIN or same user | Get user profile |
| PUT | `/api/v1/users/{id}` | TENANT_ADMIN or same user | Update user |
| DELETE | `/api/v1/users/{id}` | TENANT_ADMIN | Deactivate user |
| GET | `/api/v1/roles` | TENANT_ADMIN | List roles available in current tenant |
| POST | `/api/v1/roles` | TENANT_ADMIN | Create a custom role |
| POST | `/api/v1/roles/{id}/permissions` | TENANT_ADMIN | Assign permissions to a role |
| GET | `/api/permissions` | TENANT_ADMIN | List all available permissions |
| POST | `/api/v1/users/{id}/roles` | TENANT_ADMIN | Assign a role to a user |

Swagger: `http://localhost:8080/swagger-ui.html` → select `user-service`

---

## Multi-Tenancy

All requests include `X-Tenant-Id` header (injected by API Gateway from JWT `tenant_id` claim). `TenantFilter` reads this header into `TenantContext` (ThreadLocal). All DB queries scope to current tenant automatically.

MASTER_ADMIN has `tenant_id = null` in the JWT and bypasses tenant scoping via `SecurityService.isMasterAdmin()`.

---

## Database

Database: `taskmanager_users` (PostgreSQL 17)

Tables: `tenants`, `users`, `roles`, `permissions`, `role_permissions`, `user_roles`

Migrations in `src/main/resources/db/migration/`:
- `V1__initial_schema.sql` — creates all tables with indexes and foreign keys
- `V2__seed_roles_permissions.sql` — seeds 12 permissions and 4 system roles with role-permission mappings

---

## Keycloak Integration

`KeycloakAdminService` uses the Keycloak Admin REST API (`keycloak-admin-client:26.0.0`):

- **`createTenantAdmin()`** — creates user in `taskmaster-admin` realm, sets `tenant_id` attribute, assigns `TENANT_ADMIN` role
- **`createUser()`** — creates user in `taskmaster-app` realm, sets `tenant_id` attribute, assigns `USER` role by default

Service account credentials: `KEYCLOAK_BACKEND_SECRET` env var (mapped to `keycloak.admin.client-secret` in config).

---

## Caching (Redis)

| Cache Name | Key | TTL | Evicted On |
|---|---|---|---|
| `tenants` | tenant UUID | 30 min | update, delete |
| `roles` | tenant UUID | 60 min | role create/update |
| `permissions` | `"all"` | 24 h | permission change |
| `users` | user UUID | 10 min | update, deactivate |

---

## Security

```kotlin
// Examples of method-level security
@PreAuthorize("hasRole('MASTER_ADMIN')")
fun createTenant(...)

@PreAuthorize("hasRole('MASTER_ADMIN') or @sec.isTenantOwner(#tenantId)")
fun getTenant(tenantId: UUID, ...)

@PreAuthorize("@sec.hasPermission('USER_MANAGE') or @sec.isSameUser(#id)")
fun updateUser(id: UUID, ...)
```

JWT is validated against both Keycloak realms (`taskmaster-admin`, `taskmaster-app`) via `JwtIssuerAuthenticationManagerResolver`.

---

## Configuration

Key config properties (set via Config Server profile files):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/taskmanager_users
  data:
    redis:
      host: redis
keycloak:
  admin:
    server-url: http://keycloak:8180
    realm: master
    client-id: taskmaster-backend
    client-secret: ${KEYCLOAK_BACKEND_SECRET}
```

---

## Running Locally

```bash
# Full stack
docker compose up -d user-service

# Build and restart single service
./gradlew :user-service:build
docker compose build user-service
docker compose up -d --no-deps user-service

# Remote debug (JDWP port 5005)
# Connect IDE to localhost:5005
```

---

## Tests

Unit tests in `src/test/kotlin/com/taskmaster/user/`:

- `TenantServiceTest` — create, update, findById, duplicate domain
- `RoleServiceTest` — role listing, permission listing
- `UserServiceTest` — create user, deactivate, role assignment

```bash
./gradlew :user-service:test
```
