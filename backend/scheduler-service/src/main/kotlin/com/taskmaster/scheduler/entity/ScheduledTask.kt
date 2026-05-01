package com.taskmaster.scheduler.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "scheduled_tasks")
class ScheduledTask(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "task_id")
    val taskId: UUID,

    @Column(name = "tenant_id")
    val tenantId: UUID,

    @Column(name = "scheduled_at")
    var scheduledAt: Instant,

    @Column(name = "cron_expr")
    val cronExpr: String? = null,

    @Column(name = "job_key", unique = true)
    val jobKey: String,

    var active: Boolean = true,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
