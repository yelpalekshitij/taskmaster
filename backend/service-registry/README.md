# service-registry

Netflix Eureka Server — service discovery for all TaskMaster backend services.

**Port:** `8761`

---

## Responsibilities

- Accept registrations from all Spring Boot services on startup
- Provide a service registry that the API Gateway queries for `lb://service-name` routing
- Display a dashboard for monitoring registered instances

---

## Dashboard

`http://localhost:8761`

Shows all registered service instances, their status (UP/DOWN), and metadata (IP, port, metadata).

---

## How It Works

All backend services (`user-service`, `task-service`, `notification-service`, `scheduler-service`, `api-gateway`) are Eureka clients. On startup they POST a registration:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://service-registry:8761/eureka/
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
```

The API Gateway uses `lb://task-service` in route URIs — Spring Cloud LoadBalancer queries Eureka for live instances and round-robins requests.

---

## Security

The registry is intentionally **open** (no authentication) for local development. `SecurityConfig.kt` disables CSRF and permits all requests.

**Reason:** Spring Cloud Netflix 2024.0.x does not transmit Basic Auth credentials embedded in `defaultZone` URLs. Adding HTTP Basic Auth to the server would cause every client's registration to fail with 401. In production, protect the registry at the network layer (private subnet / VPN) rather than with application-level auth.

---

## Configuration

```yaml
server:
  port: 8761
eureka:
  client:
    register-with-eureka: false   # server does not register itself
    fetch-registry: false
  server:
    enable-self-preservation: false   # dev only; set true in prod
```

`enable-self-preservation: false` prevents the "EMERGENCY!" banner in dev when instances deregister quickly during restarts.

---

## Running Locally

service-registry must start before any other Spring Boot service.

```bash
docker compose up -d service-registry

# Check health
curl http://localhost:8761/actuator/health
```

---

## Scaling

In production, run multiple Eureka instances in peer-aware replication mode. See the [ARCHITECTURE.md](../ARCHITECTURE.md#service-discovery-and-load-balancing) section for peer configuration.
