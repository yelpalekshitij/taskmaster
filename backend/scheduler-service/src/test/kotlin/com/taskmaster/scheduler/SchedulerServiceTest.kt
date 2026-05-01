package com.taskmaster.scheduler

import com.taskmaster.scheduler.entity.ScheduledTask
import com.taskmaster.scheduler.entity.ScheduledTaskRepository
import com.taskmaster.scheduler.job.TaskSchedulerJob
import com.taskmaster.scheduler.service.SchedulerService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.Trigger
import java.time.Instant
import java.util.UUID

class SchedulerServiceTest {

    private val quartzScheduler = mockk<Scheduler>()
    private val scheduledTaskRepository = mockk<ScheduledTaskRepository>()
    private val schedulerService = SchedulerService(quartzScheduler, scheduledTaskRepository)

    @Test
    fun `scheduleTask persists scheduled task and calls quartz scheduler`() {
        val taskId = UUID.randomUUID()
        val tenantId = UUID.randomUUID()
        val scheduledAt = Instant.now().plusSeconds(3600)
        val expectedJobKey = "task-$taskId"

        every { quartzScheduler.checkExists(any<JobKey>()) } returns false
        every { quartzScheduler.scheduleJob(any<JobDetail>(), any<Trigger>()) } returns java.util.Date()

        val savedTask = ScheduledTask(
            taskId = taskId,
            tenantId = tenantId,
            scheduledAt = scheduledAt,
            jobKey = expectedJobKey
        )
        val taskSlot = slot<ScheduledTask>()
        every { scheduledTaskRepository.findByTaskId(taskId) } returns null
        every { scheduledTaskRepository.save(capture(taskSlot)) } returns savedTask

        val result = schedulerService.scheduleTask(taskId, tenantId, scheduledAt)

        verify(exactly = 1) { quartzScheduler.scheduleJob(any(), any()) }
        verify(exactly = 1) { scheduledTaskRepository.save(any()) }

        val capturedTask = taskSlot.captured
        assertEquals(taskId, capturedTask.taskId)
        assertEquals(tenantId, capturedTask.tenantId)
        assertEquals(scheduledAt, capturedTask.scheduledAt)
        assertEquals(expectedJobKey, capturedTask.jobKey)
    }

    @Test
    fun `cancelSchedule marks task inactive and deletes quartz job`() {
        val taskId = UUID.randomUUID()
        val jobKey = JobKey.jobKey("task-$taskId")
        val existingTask = ScheduledTask(
            taskId = taskId,
            tenantId = UUID.randomUUID(),
            scheduledAt = Instant.now(),
            jobKey = "task-$taskId",
            active = true
        )

        every { quartzScheduler.checkExists(jobKey) } returns true
        every { quartzScheduler.deleteJob(jobKey) } returns true
        every { scheduledTaskRepository.findByTaskId(taskId) } returns existingTask
        every { scheduledTaskRepository.save(any()) } returns existingTask

        schedulerService.cancelSchedule(taskId)

        verify(exactly = 1) { quartzScheduler.deleteJob(jobKey) }
        verify(exactly = 1) { scheduledTaskRepository.save(match { !it.active }) }
    }

    @Test
    fun `getActiveSchedules returns tasks for tenant`() {
        val tenantId = UUID.randomUUID()
        val tasks = listOf(
            ScheduledTask(
                taskId = UUID.randomUUID(),
                tenantId = tenantId,
                scheduledAt = Instant.now().plusSeconds(1800),
                jobKey = "task-${UUID.randomUUID()}",
                active = true
            )
        )

        every { scheduledTaskRepository.findByTenantIdAndActiveTrue(tenantId) } returns tasks

        val result = schedulerService.getActiveSchedules(tenantId)

        assertEquals(1, result.size)
        assertEquals(tenantId, result[0].tenantId)
        assertFalse(!result[0].active)
    }
}
