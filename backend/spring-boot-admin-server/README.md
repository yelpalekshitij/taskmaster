# spring-boot-admin-server

Spring Boot Admin — runtime monitoring dashboard for all TaskMaster services. Provides health status, log-level management, JMX beans, thread dumps, and metrics.

**Port:** `8090`

---

## Dashboard

`http://localhost:8090/admin`  
Credentials: `admin` / `admin` (set via `ADMIN_SERVER_USER` / `ADMIN_SERVER_PASSWORD` env vars)

---

## Capabilities

| Feature | Description |
|---|---|
| **Instance health** | View UP/DOWN status, health details (DB, Redis, Kafka, Eureka) |
| **Log level management** | Change log level per logger at runtime without restart |
| **HTTP traces** | Last N requests with status codes and latencies |
| **Thread dump** | Snapshot all threads per service instance |
| **Heap dump** | Trigger heap dump for OOM analysis |
| **JMX beans** | Browse and invoke JMX MBeans |
| **Metrics** | Key Micrometer metrics (JVM memory, GC, HTTP request rates) |
| **Environment** | View effective configuration properties |
| **Notifications** | Slack/email alerts on instance status change |

---

## How Services Register

All application services include `spring-boot-admin-starter-client`. They register with spring-boot-admin-server on startup:

```yaml
spring:
  boot:
    admin:
      client:
        url: http://spring-boot-admin-server:8090/admin
        username: admin
        password: ${ADMIN_SERVER_PASSWORD:admin}
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
```

---

## Security

Admin server is secured with Spring Security HTTP Basic. In production, replace with Keycloak OIDC login — see `SecurityConfig.kt` comments for the OAuth2 login alternative.

---

## Log Level Management (Example)

To enable DEBUG for `com.taskmaster.task` package at runtime:

1. Open `http://localhost:8090/admin`
2. Select the `task-service` instance
3. Go to **Loggers**
4. Search for `com.taskmaster.task`
5. Click `DEBUG`

This change is immediate and does not require a restart. It resets on next service restart unless persisted in config.

---

## Configuration

```yaml
server:
  port: 8090
  servlet:
    context-path: /admin
spring:
  boot:
    admin:
      ui:
        title: TaskMaster Admin
  security:
    user:
      name: admin
      password: admin
eureka:
  client:
    service-url:
      defaultZone: http://service-registry:8761/eureka/
```

Spring boot admin-server itself registers with Eureka so it appears as a service instance.

---

## Running Locally

```bash
docker compose up -d spring-boot-admin-server

# Note: Kafka UI runs on 8090 too — spring-boot-admin-server uses path /admin to avoid conflict
# Kafka UI: http://localhost:9091
```
