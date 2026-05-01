package com.taskmaster.task.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.taskmaster.task.comment.TaskCommentRepository
import com.taskmaster.task.history.TaskHistoryService
import com.taskmaster.task.outbox.OutboxEvent
import com.taskmaster.task.outbox.OutboxEventRepository
import com.taskmaster.task.task.dto.CreateTaskInput
import com.taskmaster.task.task.dto.TaskDto
import com.taskmaster.task.task.dto.TaskFilter
import com.taskmaster.task.task.dto.TaskPage
import com.taskmaster.task.task.dto.UpdateTaskInput
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class TaskService(
    private val taskRepository: TaskRepository,
    private val outboxRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
    private val taskHistoryService: TaskHistoryService,
    private val taskCommentRepository: TaskCommentRepository
) {
    private val log = LoggerFactory.getLogger(TaskService::class.java)

    @Transactional(readOnly = true)
    fun findAll(tenantId: UUID, filter: TaskFilter?, page: Int, size: Int): TaskPage {
        val spec = TaskSpecification.fromFilter(tenantId, filter)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = taskRepository.findAll(spec, pageable)
        return TaskPage(
            content = result.content.map { it.toDtoWithComments() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            pageNumber = result.number,
            pageSize = result.size
        )
    }

    @Transactional(readOnly = true)
    fun findById(id: UUID, tenantId: UUID): TaskDto? {
        return taskRepository.findByIdAndTenantId(id, tenantId)?.toDtoWithComments()
    }

    @Transactional(readOnly = true)
    fun findByAssignee(tenantId: UUID, assignedTo: UUID, page: Int, size: Int): TaskPage {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = taskRepository.findByAssignedToAndTenantId(assignedTo, tenantId, pageable)
        return TaskPage(
            content = result.content.map { it.toDtoWithComments() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            pageNumber = result.number,
            pageSize = result.size
        )
    }

    fun createTask(tenantId: UUID, createdBy: UUID, input: CreateTaskInput): TaskDto {
        val task = Task(
            title = input.title,
            description = input.description,
            status = TaskStatus.TODO,
            priority = input.priority ?: TaskPriority.MEDIUM,
            dueDate = input.dueDate,
            tenantId = tenantId,
            createdBy = createdBy,
            assignedTo = input.assignedTo,
            tags = input.tags?.toMutableSet() ?: mutableSetOf()
        )
        val saved = taskRepository.save(task)

        input.assignedTo?.let { assignedTo ->
            publishOutboxEvent(saved, "TASK_ASSIGNED", assignedTo)
        }

        log.info("Task created: id={}, tenantId={}", saved.id, tenantId)
        return saved.toDto()
    }

    fun updateTask(id: UUID, tenantId: UUID, input: UpdateTaskInput): TaskDto {
        val task = taskRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NoSuchElementException("Task not found: $id")

        input.title?.let { task.title = it }
        input.description?.let { task.description = it }
        input.priority?.let { task.priority = it }
        input.dueDate?.let { task.dueDate = it }
        input.tags?.let { task.tags = it.toMutableSet() }
        task.updatedAt = Instant.now()

        val saved = taskRepository.save(task)
        log.info("Task updated: id={}", id)
        return saved.toDto()
    }

    fun updateTaskStatus(id: UUID, tenantId: UUID, newStatus: TaskStatus, changedBy: UUID): TaskDto {
        val task = taskRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NoSuchElementException("Task not found: $id")
        val oldStatus = task.status
        task.status = newStatus
        task.updatedAt = Instant.now()
        val saved = taskRepository.save(task)

        taskHistoryService.record(id, changedBy, oldStatus, newStatus)

        task.assignedTo?.let { assignedTo ->
            publishOutboxEvent(saved, "TASK_UPDATED", assignedTo)
        }

        log.info("Task status updated: id={}, {} -> {}", id, oldStatus, newStatus)
        return saved.toDto()
    }

    fun assignTask(id: UUID, tenantId: UUID, assignedTo: UUID, assignedBy: UUID): TaskDto {
        val task = taskRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NoSuchElementException("Task not found: $id")
        task.assignedTo = assignedTo
        task.updatedAt = Instant.now()
        val saved = taskRepository.save(task)

        publishOutboxEvent(saved, "TASK_ASSIGNED", assignedTo)

        log.info("Task assigned: id={}, assignedTo={}", id, assignedTo)
        return saved.toDto()
    }

    fun deleteTask(id: UUID, tenantId: UUID) {
        val task = taskRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NoSuchElementException("Task not found: $id")
        taskRepository.delete(task)
        log.info("Task deleted: id={}", id)
    }

    private fun publishOutboxEvent(task: Task, eventType: String, assignedTo: UUID) {
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "eventId" to UUID.randomUUID().toString(),
                "taskId" to task.id.toString(),
                "taskTitle" to task.title,
                "tenantId" to task.tenantId.toString(),
                "assignedTo" to assignedTo.toString(),
                "eventType" to eventType,
                "timestamp" to Instant.now().toString()
            )
        )
        val idempotencyKey = "${task.id}:${eventType}:${Instant.now().epochSecond / 60}"

        try {
            outboxRepository.save(
                OutboxEvent(
                    aggregateId = task.id,
                    eventType = eventType,
                    payload = payload,
                    idempotencyKey = idempotencyKey
                )
            )
        } catch (ex: Exception) {
            log.warn("Duplicate outbox event skipped: {}", idempotencyKey)
        }
    }

    private fun Task.toDto() = TaskDto(
        id = id,
        title = title,
        description = description,
        status = status,
        priority = priority,
        dueDate = dueDate,
        scheduledDate = scheduledDate,
        tenantId = tenantId,
        assignedTo = assignedTo,
        createdBy = createdBy,
        tags = tags.toSet(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Task.toDtoWithComments(): TaskDto {
        val comments = taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(id)
            .map { c ->
                com.taskmaster.task.comment.dto.TaskCommentDto(
                    id = c.id,
                    taskId = c.taskId,
                    userId = c.userId,
                    content = c.content,
                    createdAt = c.createdAt
                )
            }
        return toDto().copy(comments = comments)
    }
}
