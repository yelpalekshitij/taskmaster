.PHONY: up down build logs ps clean infra-up infra-down

## Start all services
up:
	docker compose up -d

## Start infrastructure only (DB, Keycloak, Kafka, Redis, ELK, Prometheus, Grafana)
infra-up:
	docker compose up -d postgres redis keycloak mailhog kafka kafka-ui elasticsearch logstash kibana prometheus grafana zipkin

## Stop all services
down:
	docker compose down

## Stop and remove volumes (destructive)
clean:
	docker compose down -v --remove-orphans

## Build all backend services
build:
	./gradlew clean build -x test

## Build and start
start: build up

## Show running containers
ps:
	docker compose ps

## Follow logs for a service: make logs s=task-service
logs:
	docker compose logs -f $(s)

## Follow all logs
logs-all:
	docker compose logs -f

## Run backend tests
test:
	./gradlew test

## Rebuild a specific service: make rebuild s=task-service
rebuild:
	docker compose build $(s) && docker compose up -d $(s)
