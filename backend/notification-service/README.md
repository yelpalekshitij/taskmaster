# notification-service

Kafka consumer that processes task domain events and delivers notifications via email (Mailhog/SMTP) and FCM web push. Implements idempotency via Redis to prevent duplicate delivery.

**Port:** `8083`

---

## Responsibilities

- Consume `notification-events` Kafka topic published by task-service Outbox Relay
- Store notification records in DB (for in-app notification feed)
- Send email notifications via JavaMailSender (Mailhog in local dev)
- Send FCM push notifications via Firebase Admin SDK
- Respect per-user notification preferences (email enabled, push enabled, FCM token)
- Handle failures with retries + Dead Letter Topic

---

## Kafka Topics

| Topic | Direction | Description |
|---|---|---|
| `notification-events` | Consumed | Task domain events from task-service outbox |
| `notification-events.DLT` | Consumed (DLT consumer) | Events that failed all retry attempts |

**Consumer group:** `notification-service`

**Partition key:** `tenant_id` — ensures ordering of events per tenant

---

## Event Types Processed

| Event Type | Email Subject | Push Title |
|---|---|---|
| `TASK_ASSIGNED` | "New task assigned to you" | Task assignment notification |
| `TASK_UPDATED` | "Task updated" | Task update notification |
| `TASK_DUE` | "Task due soon" | Due date reminder |
| `TASK_SCHEDULED` | "Task scheduled for execution" | Scheduling confirmation |

---

## Idempotency

Redis `SETNX idempotency::{eventId}` with 24-hour TTL prevents duplicate processing if the same event is consumed more than once (e.g., after consumer restart before offset commit).

```kotlin
val isNew = redisTemplate.opsForValue()
    .setIfAbsent("idempotency::$eventId", "processed", Duration.ofHours(24))
if (isNew == false) {
    log.warn("Duplicate event $eventId ignored")
    return
}
```

---

## Error Handling & Retries

Configured via `DefaultErrorHandler`:
- **3 retries** with **1-second fixed backoff**
- After max retries → event published to `notification-events.DLT`
- DLT consumer logs a structured `ERROR` entry and creates a `FAILED` notification record for alerting

```kotlin
DefaultErrorHandler(
    DeadLetterPublishingRecoverer(kafkaTemplate),
    FixedBackOff(1000L, 3L)
)
```

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/notifications` | List notifications for current user (paginated) |
| PUT | `/api/v1/notifications/{id}/read` | Mark notification as read |
| PUT | `/api/v1/notifications/read-all` | Mark all as read |
| GET | `/api/v1/notifications/preferences` | Get user's notification preferences |
| PUT | `/api/v1/notifications/preferences` | Update preferences (email/push on/off, FCM token) |
| POST | `/api/v1/notifications/send` | Direct send (used by scheduler-service via Feign) |

Swagger: `http://localhost:8080/swagger-ui.html` → select `notification-service`

---

## Database

Database: `taskmanager_notifications` (PostgreSQL 17)

Tables:
- `notifications` — stored notification records (id, userId, tenantId, type, title, message, referenceId, read, createdAt)
- `notification_preferences` — per-user settings (emailEnabled, pushEnabled, fcmToken, email)

Migrations in `src/main/resources/db/migration/`:
- `V1__initial_schema.sql`

---

## Email (Mailhog)

Local dev uses Mailhog as SMTP server. Emails are rendered via Thymeleaf templates in `src/main/resources/templates/`.

Access Mailhog UI: `http://localhost:8025`

Config:
```yaml
spring:
  mail:
    host: ${MAIL_HOST:mailhog}
    port: ${MAIL_PORT:1025}
```

---

## FCM Push Notifications

Requires a Firebase service account key file. Set environment variables:

```bash
FCM_SERVICE_ACCOUNT_KEY=/path/to/serviceAccountKey.json
FCM_PROJECT_ID=your-firebase-project-id
```

If not configured, FCM calls are logged as warnings and skipped gracefully. The user-frontend service worker (`firebase-messaging-sw.js`) handles receiving push messages.

---

## Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/taskmanager_notifications
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: notification-service
      auto-offset-reset: earliest
  data:
    redis:
      host: redis
```

---

## Running Locally

```bash
docker compose up -d notification-service

./gradlew :notification-service:build
docker compose build notification-service
docker compose up -d --no-deps notification-service

# Remote debug (JDWP port 5007)
```

---

## Tests

```bash
./gradlew :notification-service:test
```

Unit tests cover: `NotificationEventConsumer` idempotency logic, `EmailService`, `NotificationService` CRUD, DLT consumer error handling.
