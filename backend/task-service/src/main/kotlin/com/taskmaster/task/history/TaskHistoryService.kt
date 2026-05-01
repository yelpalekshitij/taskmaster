package com.taskmaster.task.history

import com.taskmaster.task.task.TaskStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TaskHistoryService(private val taskHistoryRepository: TaskHistoryRepository) {
    private val log = LoggerFactory.getLogger(TaskHistoryService::class.java)

    @Transactional(propagation = Propagation.MANDATORY)
    fun record(taskId: UUID, changedBy: UUID, oldStatus: TaskStatus, newStatus: TaskStatus, comment: String? = null) {
        val history = TaskHistory(
            taskId = taskId,
            changedBy = changedBy,
            oldStatus = oldStatus,
            newStatus = newStatus,
            comment = comment
        )
        taskHistoryRepository.save(history)
        log.debug("History recorded: taskId={}, {} -> {}", taskId, oldStatus, newStatus)
    }

    @Transactional(readOnly = true)
    fun findByTaskId(taskId: UUID): List<TaskHistory> =
        taskHistoryRepository.findByTaskIdOrderByChangedAtDesc(taskId)
}
