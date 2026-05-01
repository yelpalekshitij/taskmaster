package com.taskmaster.task.comment

import com.taskmaster.task.comment.dto.TaskCommentDto
import com.taskmaster.task.task.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class TaskCommentService(
    private val taskCommentRepository: TaskCommentRepository,
    private val taskRepository: TaskRepository
) {
    private val log = LoggerFactory.getLogger(TaskCommentService::class.java)

    @Transactional(readOnly = true)
    fun findByTaskId(taskId: UUID): List<TaskCommentDto> {
        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
            .map { it.toDto() }
    }

    fun addComment(taskId: UUID, tenantId: UUID, userId: UUID, content: String): TaskCommentDto {
        require(taskRepository.existsByIdAndTenantId(taskId, tenantId)) {
            "Task not found: $taskId"
        }

        val comment = TaskComment(
            taskId = taskId,
            userId = userId,
            content = content
        )
        val saved = taskCommentRepository.save(comment)
        log.info("Comment added to task: taskId={}, commentId={}", taskId, saved.id)
        return saved.toDto()
    }

    fun deleteComment(commentId: UUID, userId: UUID) {
        val comment = taskCommentRepository.findById(commentId)
            .orElseThrow { NoSuchElementException("Comment not found: $commentId") }

        require(comment.userId == userId) { "You can only delete your own comments" }
        taskCommentRepository.delete(comment)
        log.info("Comment deleted: id={}", commentId)
    }

    private fun TaskComment.toDto() = TaskCommentDto(
        id = id,
        taskId = taskId,
        userId = userId,
        content = content,
        createdAt = createdAt
    )
}
