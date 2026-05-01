package com.taskmaster.task.history

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TaskHistoryRepository : JpaRepository<TaskHistory, UUID> {

    fun findByTaskIdOrderByChangedAtDesc(taskId: UUID): List<TaskHistory>
}
