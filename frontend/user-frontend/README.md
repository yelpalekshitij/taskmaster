# TaskMaster User Frontend

Angular 19 standalone application for end users to manage tasks.

## Features

- **Dashboard** — stats overview, recent tasks
- **Task List** — paginated table with status/priority filters, create/update tasks
- **Kanban Board** — drag-and-drop task status management
- **Notifications** — in-app notification center with mark-as-read
- **Profile** — user info and notification preferences

## Stack

- Angular 19 (standalone components)
- Angular Material 19
- Angular CDK (drag-and-drop)
- apollo-angular + Apollo Client (GraphQL)
- angular-auth-oidc-client (OIDC/Keycloak)
- NgRx (store ready for state expansion)

## Development

```bash
npm install
npm start        # http://localhost:4200
npm run build    # production build
```

## Docker

```bash
docker build -t taskmaster-user-frontend .
docker run -p 80:80 taskmaster-user-frontend
```

## Environment

Configure `src/environments/environment.ts`:
- `apiUrl` — backend API gateway URL
- `graphqlUrl` — GraphQL endpoint
- `keycloak.authority` — Keycloak realm URL
- `keycloak.clientId` — OIDC client ID
