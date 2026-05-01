package com.taskmaster.scheduler.job

import com.taskmaster.scheduler.feign.TaskServiceClient
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TaskSchedulerJob(private val taskServiceClient: TaskServiceClient) : Job {

    private val log = LoggerFactory.getLogger(TaskSchedulerJob::class.java)

    override fun execute(context: JobExecutionContext) {
        val taskId = context.mergedJobDataMap.getString("taskId")
        val tenantId = context.mergedJobDataMap.getString("tenantId")
        try {
            taskServiceClient.updateTaskStatus(UUID.fromString(taskId), "SCHEDULED", tenantId)
            log.info("Task scheduled: taskId={}", taskId)
        } catch (ex: Exception) {
            log.error("Failed to update task status: taskId={}, error={}", taskId, ex.message, ex)
            throw ex
        }
    }
}
