package com.taskmaster.scheduler.job

import com.taskmaster.scheduler.feign.NotificationServiceClient
import com.taskmaster.scheduler.feign.SendNotificationRequest
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TaskDueReminderJob(private val notificationClient: NotificationServiceClient) : Job {

    private val log = LoggerFactory.getLogger(TaskDueReminderJob::class.java)

    override fun execute(context: JobExecutionContext) {
        val taskId = context.mergedJobDataMap.getString("taskId")
        val tenantId = context.mergedJobDataMap.getString("tenantId")
        val assignedTo = context.mergedJobDataMap.getString("assignedTo")
        try {
            notificationClient.sendDirectNotification(
                SendNotificationRequest(
                    userId = UUID.fromString(assignedTo),
                    tenantId = UUID.fromString(tenantId),
                    type = "TASK_DUE",
                    title = "Task due soon",
                    message = "Your task is due soon",
                    referenceId = UUID.fromString(taskId)
                )
            )
            log.info("Due reminder sent: taskId={}, assignedTo={}", taskId, assignedTo)
        } catch (ex: Exception) {
            log.error("Failed to send due reminder: taskId={}, error={}", taskId, ex.message, ex)
            throw ex
        }
    }
}
