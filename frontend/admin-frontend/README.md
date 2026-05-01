# TaskMaster Admin Frontend

Angular 19 standalone application for administrators to manage tenants, users, roles, and view reports.

## Features

- **Dashboard** — role-aware: MASTER_ADMIN sees all tenants, TENANT_ADMIN sees their tenant
- **Tenant Management** — create, activate/deactivate tenants (MASTER_ADMIN only)
- **User Management** — list users, assign roles, activate/deactivate
- **Role Management** — view and create custom roles with permissions
- **Reports** — global and per-tenant task completion statistics

## Stack

- Angular 19 (standalone components)
- Angular Material 19 with purple theme (admin aesthetic)
- angular-auth-oidc-client (OIDC/Keycloak, separate realm: `taskmaster-admin`)
- NgRx (store ready for state expansion)
- REST-only (no GraphQL)

## Roles

| Role | Access |
|------|--------|
| `MASTER_ADMIN` | Everything: all tenants, global stats |
| `TENANT_ADMIN` | Own tenant: users, roles, reports |

## Development

```bash
npm install
npm start        # http://localhost:4201
npm run build    # production build
```

## Docker

```bash
docker build -t taskmaster-admin-frontend .
docker run -p 80:80 taskmaster-admin-frontend
```

## Environment

Configure `src/environments/environment.ts`:
- `apiUrl` — backend API gateway URL
- `keycloak.authority` — Keycloak realm URL (taskmaster-admin realm)
- `keycloak.clientId` — OIDC client ID (taskmaster-admin-frontend)
