package com.taskmaster.notification.notification

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class NotificationType {
    TASK_ASSIGNED, TASK_UPDATED, TASK_DUE, TASK_SCHEDULED, SYSTEM
}

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id")
    val userId: UUID,

    @Column(name = "tenant_id")
    val tenantId: UUID,

    @Enumerated(EnumType.STRING)
    val type: NotificationType,

    val title: String,

    val message: String,

    @Column(name = "reference_id")
    val referenceId: UUID? = null,

    var read: Boolean = false,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
