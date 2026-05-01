#!/bin/bash
# Creates Kafka topics for TaskMaster
# Runs on Kafka container startup via docker-compose command

set -e

KAFKA_BROKER="kafka:9092"

echo "Waiting for Kafka to be ready..."
cub kafka-ready -b $KAFKA_BROKER 1 40

echo "Creating TaskMaster Kafka topics..."

# Main notification events topic (6 partitions for tenant-based partitioning)
kafka-topics --bootstrap-server $KAFKA_BROKER \
  --create --if-not-exists \
  --topic notification-events \
  --partitions 6 \
  --replication-factor 1 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete

# Dead letter topic
kafka-topics --bootstrap-server $KAFKA_BROKER \
  --create --if-not-exists \
  --topic notification-events.DLT \
  --partitions 1 \
  --replication-factor 1 \
  --config retention.ms=2592000000

echo "Kafka topics created successfully:"
kafka-topics --bootstrap-server $KAFKA_BROKER --list
