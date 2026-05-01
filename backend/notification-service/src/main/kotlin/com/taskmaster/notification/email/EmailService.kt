package com.taskmaster.notification.email

import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: SpringTemplateEngine
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    fun sendNotification(to: String, subject: String, message: String, eventType: String) {
        try {
            val context = Context()
            context.setVariable("subject", subject)
            context.setVariable("message", message)
            context.setVariable("eventType", eventType)

            val html = templateEngine.process("notification", context)

            val msg: MimeMessage = mailSender.createMimeMessage()
            MimeMessageHelper(msg, true, "UTF-8").apply {
                setTo(to)
                setSubject(subject)
                setText(html, true)
            }
            mailSender.send(msg)
            log.info("Email sent to: {}", to)
        } catch (ex: Exception) {
            log.error("Failed to send email to {}: {}", to, ex.message, ex)
        }
    }
}
