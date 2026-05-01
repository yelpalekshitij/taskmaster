package com.taskmaster.task.comment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "task_comments")
class TaskComment(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "task_id", nullable = false)
    val taskId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,

    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now()
)
