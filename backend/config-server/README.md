# config-server

Spring Cloud Config Server — centralized configuration for all backend services. Uses a local filesystem backend with profile-based overrides.

**Port:** `8888`

---

## Responsibilities

- Serve externalized configuration to all Spring Boot microservices
- Support four deployment profiles: `local`, `dev`, `stage`, `prod`
- Allow configuration changes without service restart (via `/actuator/refresh` + `@RefreshScope`)

---

## Config File Resolution Order

Config Server merges properties from multiple files. Later files override earlier ones:

```
application.yml                    ← shared defaults (all services, all profiles)
application-{profile}.yml          ← shared profile overrides
{service-name}.yml                 ← service-specific defaults
{service-name}-{profile}.yml       ← service + profile overrides (highest priority)
```

Example for `task-service` with `local` profile:
```
application.yml → application-local.yml → task-service.yml → task-service-local.yml
```

---

## Config Files Location

```
config-server/config-files/
├── application.yml                    # Eureka, Actuator, Zipkin, Micrometer
├── application-local.yml              # Local Docker hostnames
├── application-dev.yml                # Dev env placeholders
├── application-stage.yml              # Stage env placeholders
├── application-prod.yml               # Prod env (secrets from env vars)
├── api-gateway.yml
├── api-gateway-local.yml
├── user-service.yml
├── user-service-local.yml
├── task-service.yml
├── task-service-local.yml
├── notification-service.yml
├── notification-service-local.yml
├── scheduler-service.yml
└── scheduler-service-local.yml
```

---

## How Services Connect

Each service bootstraps with:

```yaml
# bootstrap.yml (or spring.config.import)
spring:
  application:
    name: task-service          # used to look up task-service.yml
  config:
    import: "optional:configserver:http://config-server:8888"
  profiles:
    active: local               # set via SPRING_PROFILES_ACTIVE env var
```

`optional:` prefix means the service starts even if config-server is temporarily unavailable (useful during rolling restarts).

---

## Shared `application.yml` Defaults

Properties defined here apply to all services unless overridden:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://service-registry:8761/eureka/
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers,refresh
  tracing:
    sampling:
      probability: 1.0
spring:
  zipkin:
    base-url: http://zipkin:9411
```

---

## Adding a New Service Config

1. Create `config-files/{new-service}.yml` with service-specific defaults
2. Create `config-files/{new-service}-local.yml` with local Docker hostnames
3. Create profile variants as needed (`-dev`, `-stage`, `-prod`)
4. In the new service's `application.yml`, set `spring.application.name: new-service`

---

## Configuration

```yaml
server:
  port: 8888
spring:
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config-files/
  profiles:
    active: native
```

---

## Running Locally

config-server must start after service-registry and before application services.

```bash
docker compose up -d config-server

# Verify a service config is served
curl http://localhost:8888/task-service/local
```
