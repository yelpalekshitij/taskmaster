# scheduler-service

Quartz-based task scheduling service. Allows tasks to be scheduled for future execution and sends due-date reminders. Clustered via JDBC job store for high availability.

**Port:** `8084`

---

## Responsibilities

- Schedule tasks for execution at a specific date/time or on a recurring cron expression
- Execute scheduled tasks by calling task-service to transition status to `IN_PROGRESS`
- Send due-date reminder notifications via notification-service
- Clustered Quartz job store — multiple instances safely share one PostgreSQL-backed job table

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/scheduler/tasks` | Schedule a task for execution |
| GET | `/api/v1/scheduler/tasks` | List scheduled tasks for current tenant |
| GET | `/api/v1/scheduler/tasks/{id}` | Get schedule details |
| DELETE | `/api/v1/scheduler/tasks/{id}` | Cancel scheduled task |
| POST | `/api/v1/scheduler/reminders` | Schedule a due-date reminder |

Swagger: `http://localhost:8080/swagger-ui.html` → select `scheduler-service`

---

## Quartz Jobs

### `TaskSchedulerJob`

Triggers at the scheduled time. Reads `taskId` and `tenantId` from `JobDataMap`. Calls `TaskServiceClient.updateTaskStatus(taskId, IN_PROGRESS)` via Feign. Marks the `scheduled_tasks` record as `active = false` after execution.

### `TaskDueReminderJob`

Triggers based on a configurable offset before task due date (default: 1 hour). Calls `NotificationServiceClient.sendDirectNotification()` with event type `TASK_DUE`.

---

## Quartz Clustering

Quartz uses Spring Boot's `LocalDataSourceJobStore` (wired automatically via `SchedulerFactoryBean.setDataSource()`) backed by the `taskmanager_scheduler` PostgreSQL database. Do NOT set `org.quartz.jobStore.class` in properties — that bypasses Spring's DataSource injection and causes `DataSource name not set` on startup.

For multi-instance deployment, change `isClustered` to `true` and scale:
```bash
docker compose up -d --scale scheduler-service=2
```

---

## Service-to-Service Calls (Feign)

### `TaskServiceClient`

```kotlin
@FeignClient(name = "task-service")
interface TaskServiceClient {
    @PutMapping("/api/tasks/{taskId}/status")
    fun updateTaskStatus(@PathVariable taskId: UUID, @RequestBody request: UpdateStatusRequest)
}
```

### `NotificationServiceClient`

```kotlin
@FeignClient(name = "notification-service")
interface NotificationServiceClient {
    @PostMapping("/api/notifications/send")
    fun sendDirectNotification(@RequestBody request: SendNotificationRequest)
}
```

Feign clients use Keycloak Client Credentials flow for background jobs (machine-to-machine JWT). A `RequestInterceptor` bean fetches a fresh token from Keycloak and adds it as `Authorization: Bearer ...` header.

---

## Database

Database: `taskmanager_scheduler` (PostgreSQL 17)

Tables:
- `scheduled_tasks` — application-level record (taskId, tenantId, scheduledAt, cronExpr, jobKey, active)
- Quartz internal tables — auto-created via `spring.quartz.jdbc.initialize-schema: always` (or existing Flyway migration)

Migrations in `src/main/resources/db/migration/`:
- `V1__initial_schema.sql`

---

## Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/taskmanager_scheduler
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: always   # creates QRTZ_* tables on first start
    properties:
      # Do NOT set org.quartz.jobStore.class — Spring Boot injects LocalDataSourceJobStore
      org.quartz.scheduler.instanceName: TaskMasterScheduler
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.jobStore.driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
      org.quartz.jobStore.tablePrefix: QRTZ_
      org.quartz.jobStore.isClustered: false   # set true when scaling to multiple instances
      org.quartz.threadPool.threadCount: 5
```

---

## Running Locally

```bash
docker compose up -d scheduler-service

./gradlew :scheduler-service:build
docker compose build scheduler-service
docker compose up -d --no-deps scheduler-service

# Remote debug (JDWP port 5008)
```

---

## Tests

```bash
./gradlew :scheduler-service:test
```

Unit tests cover: `SchedulerService` (schedule/cancel), `TaskSchedulerJob` execution logic, Feign client mock integration.
