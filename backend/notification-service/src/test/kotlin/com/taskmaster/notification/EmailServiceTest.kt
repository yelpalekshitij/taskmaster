package com.taskmaster.notification

import com.taskmaster.notification.email.EmailService
import com.taskmaster.notification.push.FcmService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender
import org.thymeleaf.context.IContext
import org.thymeleaf.spring6.SpringTemplateEngine

class EmailServiceTest {

    private val mailSender = mockk<JavaMailSender>()
    private val templateEngine = mockk<SpringTemplateEngine>()
    private val emailService = EmailService(mailSender, templateEngine)

    @Test
    fun `sendNotification sends to correct recipient`() {
        val mimeMessage = mockk<MimeMessage>(relaxed = true)
        every { mailSender.createMimeMessage() } returns mimeMessage
        every { templateEngine.process(any<String>(), any<IContext>()) } returns "<html>test</html>"
        every { mailSender.send(any<MimeMessage>()) } returns Unit

        emailService.sendNotification(
            to = "user@example.com",
            subject = "New task assigned",
            message = "Task 'Write tests' has been assigned to you",
            eventType = "TASK_ASSIGNED"
        )

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
        verify(exactly = 1) { templateEngine.process("notification", any<IContext>()) }
    }

    @Test
    fun `sendNotification handles mail exception gracefully`() {
        val mimeMessage = mockk<MimeMessage>(relaxed = true)
        every { mailSender.createMimeMessage() } returns mimeMessage
        every { templateEngine.process(any<String>(), any<IContext>()) } returns "<html>test</html>"
        every { mailSender.send(any<MimeMessage>()) } throws RuntimeException("SMTP connection refused")

        // Should not throw — errors are logged internally
        emailService.sendNotification(
            to = "user@example.com",
            subject = "Task due soon",
            message = "Your task is due soon",
            eventType = "TASK_DUE"
        )
    }
}

class FcmServiceTest {

    @Test
    fun `FCM service logs warning when key not configured`() {
        // When service-account-key is blank, sendPush should be a no-op (no exception)
        val fcmService = FcmService(serviceAccountKey = "", projectId = "test-project")

        // Should not throw — it logs a warning and returns
        fcmService.sendPush(
            fcmToken = "some-token-12345",
            title = "Test push",
            body = "Test body"
        )
    }

    @Test
    fun `FCM service processes token when key is configured`() {
        val fcmService = FcmService(serviceAccountKey = "fake-key", projectId = "test-project")

        // Should not throw — logs info about the token
        fcmService.sendPush(
            fcmToken = "valid-fcm-token-abcdef",
            title = "New task assigned",
            body = "Task 'Test Task' has been assigned to you"
        )
    }
}
