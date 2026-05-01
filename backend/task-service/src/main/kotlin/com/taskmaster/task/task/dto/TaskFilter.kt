package com.taskmaster.task.task.dto

import com.taskmaster.task.task.TaskPriority
import com.taskmaster.task.task.TaskStatus

data class TaskFilter(
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val assignedTo: String? = null
)
