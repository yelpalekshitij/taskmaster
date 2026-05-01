package com.taskmaster.task.history

import com.taskmaster.task.task.TaskStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "task_history")
class TaskHistory(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "task_id", nullable = false)
    val taskId: UUID,

    @Column(name = "changed_by", nullable = false)
    val changedBy: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    val oldStatus: TaskStatus?,

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    val newStatus: TaskStatus,

    @Column(columnDefinition = "TEXT")
    val comment: String? = null,

    @Column(name = "changed_at", updatable = false)
    val changedAt: Instant = Instant.now()
)
