package com.taskmaster.task.task.dto

import com.taskmaster.task.task.TaskPriority
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateTaskInput(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 500, message = "Title must not exceed 500 characters")
    val title: String,

    val description: String? = null,

    val priority: TaskPriority? = null,

    val dueDate: Instant? = null,

    val assignedTo: UUID? = null,

    val tags: List<String>? = null
)
