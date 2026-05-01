package com.taskmaster.notification.preference

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification_preferences")
class NotificationPreference(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", unique = true)
    val userId: UUID,

    @Column(name = "tenant_id")
    val tenantId: UUID,

    var emailEnabled: Boolean = true,

    var pushEnabled: Boolean = true,

    @Column(name = "fcm_token")
    var fcmToken: String? = null,

    var email: String,

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
