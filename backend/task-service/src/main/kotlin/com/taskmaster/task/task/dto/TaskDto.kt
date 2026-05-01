package com.taskmaster.task.task.dto

import com.taskmaster.task.comment.dto.TaskCommentDto
import com.taskmaster.task.task.TaskPriority
import com.taskmaster.task.task.TaskStatus
import java.time.Instant
import java.util.UUID

data class TaskDto(
    val id: UUID,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueDate: Instant?,
    val scheduledDate: Instant?,
    val tenantId: UUID,
    val assignedTo: UUID?,
    val createdBy: UUID,
    val tags: Set<String>,
    val comments: List<TaskCommentDto> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

data class TaskPage(
    val content: List<TaskDto>,
    val totalElements: Long,
    val totalPages: Int,
    val pageNumber: Int,
    val pageSize: Int
)
