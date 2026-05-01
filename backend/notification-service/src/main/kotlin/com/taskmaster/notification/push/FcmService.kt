package com.taskmaster.notification.push

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class FcmService(
    @Value("\${fcm.service-account-key:}") private val serviceAccountKey: String,
    @Value("\${fcm.project-id:your-project-id}") private val projectId: String
) {
    private val log = LoggerFactory.getLogger(FcmService::class.java)

    fun sendPush(fcmToken: String, title: String, body: String) {
        if (serviceAccountKey.isBlank()) {
            log.warn("FCM service account key not configured. Push not sent.")
            return
        }
        // FCM v1 API via HTTP POST
        // In production: initialize FirebaseApp with service account and use FirebaseMessaging
        log.info("Push notification would be sent to token: {}...", fcmToken.take(10))
    }
}
