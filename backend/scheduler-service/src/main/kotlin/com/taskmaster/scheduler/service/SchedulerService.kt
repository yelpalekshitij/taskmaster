package com.taskmaster.scheduler.service

import com.taskmaster.scheduler.entity.ScheduledTask
import com.taskmaster.scheduler.entity.ScheduledTaskRepository
import com.taskmaster.scheduler.job.TaskSchedulerJob
import org.quartz.JobBuilder
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class SchedulerService(
    private val scheduler: Scheduler,
    private val scheduledTaskRepository: ScheduledTaskRepository
) {
    private val log = LoggerFactory.getLogger(SchedulerService::class.java)

    fun scheduleTask(taskId: UUID, tenantId: UUID, scheduledAt: Instant): ScheduledTask {
        val jobKey = "task-$taskId"

        val jobDetail = JobBuilder.newJob(TaskSchedulerJob::class.java)
            .withIdentity(jobKey)
            .usingJobData("taskId", taskId.toString())
            .usingJobData("tenantId", tenantId.toString())
            .storeDurably()
            .build()

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity("trigger-$jobKey")
            .startAt(Date.from(scheduledAt))
            .forJob(jobDetail)
            .build()

        if (scheduler.checkExists(JobKey.jobKey(jobKey))) {
            scheduler.deleteJob(JobKey.jobKey(jobKey))
        }

        scheduler.scheduleJob(jobDetail, trigger)
        log.info("Quartz job scheduled: jobKey={}, scheduledAt={}", jobKey, scheduledAt)

        val existing = scheduledTaskRepository.findByTaskId(taskId)
        val scheduledTask = existing?.apply {
            this.scheduledAt = scheduledAt
            this.active = true
        } ?: ScheduledTask(
            taskId = taskId,
            tenantId = tenantId,
            scheduledAt = scheduledAt,
            jobKey = jobKey
        )

        return scheduledTaskRepository.save(scheduledTask)
    }

    fun cancelSchedule(taskId: UUID) {
        val jobKey = JobKey.jobKey("task-$taskId")
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey)
            log.info("Quartz job cancelled: taskId={}", taskId)
        }
        scheduledTaskRepository.findByTaskId(taskId)?.let {
            it.active = false
            scheduledTaskRepository.save(it)
        }
    }

    fun getActiveSchedules(tenantId: UUID): List<ScheduledTask> {
        return scheduledTaskRepository.findByTenantIdAndActiveTrue(tenantId)
    }
}
