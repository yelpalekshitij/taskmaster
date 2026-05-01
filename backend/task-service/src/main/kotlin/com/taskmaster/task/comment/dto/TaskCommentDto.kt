package com.taskmaster.task.comment.dto

import java.time.Instant
import java.util.UUID

data class TaskCommentDto(
    val id: UUID,
    val taskId: UUID,
    val userId: UUID,
    val content: String,
    val createdAt: Instant
)
