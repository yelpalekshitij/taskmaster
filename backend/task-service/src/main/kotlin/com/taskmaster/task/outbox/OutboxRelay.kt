package com.taskmaster.task.outbox

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OutboxRelay(
    private val outboxRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${outbox.relay.batch-size:50}") private val batchSize: Int
) {
    private val log = LoggerFactory.getLogger(OutboxRelay::class.java)

    @Scheduled(fixedDelayString = "\${outbox.relay.interval-ms:500}")
    @Transactional
    fun relayEvents() {
        val unpublished = outboxRepository.findUnpublished(batchSize)
        if (unpublished.isEmpty()) return

        unpublished.forEach { event ->
            try {
                kafkaTemplate.send("notification-events", event.aggregateId.toString(), event.payload).get()
                event.publishedAt = Instant.now()
                event.publishAttempts++
                outboxRepository.save(event)
                log.info("Outbox event published: id={}, type={}", event.id, event.eventType)
            } catch (ex: Exception) {
                event.publishAttempts++
                outboxRepository.save(event)
                log.error("Failed to publish outbox event: id={}", event.id, ex)
            }
        }
    }
}
