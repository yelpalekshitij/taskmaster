package com.taskmaster.notification.kafka

import com.taskmaster.notification.notification.NotificationRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class DltConsumer(private val notificationRepository: NotificationRepository) {

    private val log = LoggerFactory.getLogger(DltConsumer::class.java)

    @KafkaListener(topics = ["notification-events.DLT"], groupId = "notification-service-dlt")
    fun handleDlt(record: ConsumerRecord<String, String>) {
        log.error(
            "DLT message received: topic={}, key={}, value={}",
            record.topic(),
            record.key(),
            record.value()
        )
    }
}
