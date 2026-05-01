# task-service

Core task management service. Exposes a REST API and a Spring GraphQL endpoint. Publishes domain events via the Transactional Outbox Pattern to Kafka for async notifications.

**Port:** `8082`

---

## Responsibilities

- Task CRUD (create, read, update, delete, status transitions)
- Task assignment to users within a tenant
- Comments and tags on tasks
- Task history (status change audit log)
- Kafka Outbox — reliable async event publishing for notifications

---

## API Surfaces

### GraphQL (primary for user-frontend)

Endpoint: `POST /graphql`  
Playground: `http://localhost:8080/graphiql`

**Queries:**
```graphql
tasks(tenantId: ID!, filter: TaskFilter, page: Int, size: Int): TaskPage
myTasks(page: Int, size: Int): TaskPage
task(id: ID!): Task
```

**Mutations:**
```graphql
createTask(input: CreateTaskInput!): Task
updateTask(id: ID!, input: UpdateTaskInput!): Task
deleteTask(id: ID!): Boolean
updateTaskStatus(id: ID!, status: TaskStatus!): Task
assignTask(id: ID!, userId: ID!): Task
addComment(taskId: ID!, content: String!): TaskComment
```

### REST (for inter-service and admin)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/tasks` | List tasks with filters (paginated) |
| POST | `/api/v1/tasks` | Create task |
| GET | `/api/v1/tasks/{id}` | Get task |
| PUT | `/api/v1/tasks/{id}` | Update task |
| DELETE | `/api/v1/tasks/{id}` | Delete task |
| PUT | `/api/v1/tasks/{id}/status` | Transition task status |
| PUT | `/api/v1/tasks/{id}/assign` | Assign task to user |
| GET | `/api/v1/tasks/{id}/history` | Get status change history |
| POST | `/api/v1/tasks/{id}/comments` | Add comment |
| GET | `/api/v1/tasks/{id}/comments` | Get comments |

Swagger: `http://localhost:8080/swagger-ui.html` → select `task-service`

---

## Task Status Transitions

```
TODO → IN_PROGRESS → ON_HOLD → IN_PROGRESS
             ↓
           DONE
TODO / IN_PROGRESS → SCHEDULED
SCHEDULED → IN_PROGRESS
```

---

## Kafka Outbox Pattern

Task mutations that trigger notifications (`assignTask`, `updateTaskStatus`, `createTask`) write to the `outbox_events` table **in the same DB transaction** as the task change.

`OutboxRelay` (`@Scheduled` every 500ms) polls for unpublished events and publishes to the `notification-events` Kafka topic, then marks them as published.

**Idempotency key**: `"${taskId}:${eventType}:${epochSecond/60}"` — prevents duplicate outbox inserts within a 1-minute window if the same operation is retried.

**Outbox fields:**
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `aggregate_type` | VARCHAR | `'TASK'` |
| `aggregate_id` | UUID | task ID |
| `event_type` | VARCHAR | `TASK_ASSIGNED`, `TASK_UPDATED`, `TASK_DUE` |
| `payload` | JSONB | Full event context (taskId, title, tenantId, assignedTo, etc.) |
| `idempotency_key` | VARCHAR UNIQUE | Prevents duplicate inserts |
| `published_at` | TIMESTAMP | `NULL` until published |
| `publish_attempts` | INT | Incremented on relay failure |

---

## Database

Database: `taskmanager_tasks` (PostgreSQL 17)

Tables: `tasks`, `task_comments`, `task_tags`, `task_history`, `outbox_events`

Migrations in `src/main/resources/db/migration/`:
- `V1__initial_schema.sql` — all tables, enums (`task_status`, `task_priority`), indexes

---

## Filtering & Pagination (REST)

Admin list endpoints accept `Pageable` + filter params:

| Param | Description |
|---|---|
| `status` | Filter by task status |
| `priority` | Filter by priority |
| `assignedTo` | Filter by assigned user ID |
| `dueDateFrom` | Lower bound for due date |
| `dueDateTo` | Upper bound for due date |
| `page` / `size` | Pagination |

Implemented via `TaskSpecification` using JPA `Specification<Task>` for dynamic predicate composition.

---

## Security

```kotlin
@PreAuthorize("@sec.hasPermission('TASK_CREATE')")
fun createTask(...)

@PreAuthorize("@sec.hasPermission('TASK_READ')")
fun getTasks(...)

@PreAuthorize("@sec.hasPermission('TASK_ASSIGN')")
fun assignTask(...)
```

GraphQL resolvers and REST controllers both use `@PreAuthorize` via `@EnableMethodSecurity`.

---

## Configuration

Key config properties:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/taskmanager_tasks
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      transaction-id-prefix: task-service-tx
outbox:
  relay:
    interval-ms: 500
    batch-size: 50
```

---

## Running Locally

```bash
docker compose up -d task-service

# Rebuild
./gradlew :task-service:build
docker compose build task-service
docker compose up -d --no-deps task-service

# Remote debug (JDWP port 5006)
```

---

## Tests

```bash
./gradlew :task-service:test
```

Unit tests cover: `TaskService`, `OutboxRelay`, `TaskSpecification` filter logic, GraphQL resolver authorization.
