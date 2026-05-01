package com.taskmaster.notification.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.taskmaster.notification.email.EmailService
import com.taskmaster.notification.notification.Notification
import com.taskmaster.notification.notification.NotificationRepository
import com.taskmaster.notification.notification.NotificationType
import com.taskmaster.notification.preference.NotificationPreferenceRepository
import com.taskmaster.notification.push.FcmService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class NotificationEventConsumer(
    private val emailService: EmailService,
    private val fcmService: FcmService,
    private val preferenceRepository: NotificationPreferenceRepository,
    private val notificationRepository: NotificationRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(NotificationEventConsumer::class.java)

    @KafkaListener(topics = ["notification-events"], groupId = "notification-service")
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = objectMapper.readTree(record.value())
        val eventId = payload["eventId"]?.asText() ?: run {
            log.warn("Received event without eventId, skipping")
            return
        }
        val idempotencyKey = "idempotency::$eventId"

        // Idempotency check
        val alreadyProcessed = redisTemplate.opsForValue()
            .setIfAbsent(idempotencyKey, "processed", Duration.ofHours(24))
        if (alreadyProcessed == false) {
            log.warn("Duplicate event ignored: eventId={}", eventId)
            return
        }

        val eventType = payload["eventType"]?.asText() ?: run {
            log.warn("Event missing eventType: eventId={}", eventId)
            return
        }
        val assignedToText = payload["assignedTo"]?.asText()
        val userId = assignedToText?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: run {
            log.warn("Event missing or invalid assignedTo: eventId={}", eventId)
            return
        }
        val tenantIdText = payload["tenantId"]?.asText() ?: run {
            log.warn("Event missing tenantId: eventId={}", eventId)
            return
        }
        val tenantId = runCatching { UUID.fromString(tenantIdText) }.getOrNull() ?: run {
            log.warn("Invalid tenantId: {}", tenantIdText)
            return
        }

        val preference = preferenceRepository.findByUserId(userId).orElse(null) ?: run {
            log.warn("No notification preference found for userId={}", userId)
            return
        }

        val (title, message) = buildNotificationContent(eventType, payload)

        // Determine notification type
        val notificationType = runCatching { NotificationType.valueOf(eventType) }
            .getOrDefault(NotificationType.SYSTEM)

        // Persist notification
        notificationRepository.save(
            Notification(
                userId = userId,
                tenantId = tenantId,
                type = notificationType,
                title = title,
                message = message,
                referenceId = payload["taskId"]?.asText()?.let {
                    runCatching { UUID.fromString(it) }.getOrNull()
                }
            )
        )

        if (preference.emailEnabled && preference.email.isNotBlank()) {
            emailService.sendNotification(preference.email, title, message, eventType)
        }
        if (preference.pushEnabled && !preference.fcmToken.isNullOrBlank()) {
            fcmService.sendPush(preference.fcmToken!!, title, message)
        }

        log.info("Notification delivered: eventId={}, userId={}, type={}", eventId, userId, eventType)
    }

    private fun buildNotificationContent(eventType: String, payload: JsonNode): Pair<String, String> =
        when (eventType) {
            "TASK_ASSIGNED" -> "New task assigned" to
                    "Task '${payload["taskTitle"]?.asText()}' has been assigned to you"
            "TASK_UPDATED" -> "Task updated" to
                    "Task '${payload["taskTitle"]?.asText()}' has been updated"
            "TASK_DUE" -> "Task due soon" to
                    "Task '${payload["taskTitle"]?.asText()}' is due soon"
            else -> "Notification" to "You have a new notification"
        }
}
