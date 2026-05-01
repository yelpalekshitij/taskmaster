package com.taskmaster.notification.preference

import com.taskmaster.notification.common.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class UpdatePreferenceRequest(
    val emailEnabled: Boolean = true,
    val pushEnabled: Boolean = false,
    val fcmToken: String? = null,
    @field:Email val email: String? = null
)

data class PreferenceResponse(
    val id: UUID,
    val userId: UUID,
    val emailEnabled: Boolean,
    val pushEnabled: Boolean,
    val fcmToken: String?,
    val email: String,
    val updatedAt: Instant
)

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@Tag(name = "Notification Preferences", description = "Manage notification delivery preferences")
class PreferenceController(
    private val preferenceRepository: NotificationPreferenceRepository
) {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's notification preferences")
    fun getPreferences(): ResponseEntity<PreferenceResponse> {
        val userId = UUID.fromString(
            SecurityUtils.getCurrentUserId() ?: return ResponseEntity.status(401).build()
        )
        val tenantId = SecurityUtils.getCurrentTenantIdAsUUID()
            ?: return ResponseEntity.status(400).build()

        val preference = preferenceRepository.findByUserId(userId).orElseGet {
            NotificationPreference(
                userId = userId,
                tenantId = tenantId,
                email = ""
            )
        }
        return ResponseEntity.ok(preference.toResponse())
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update current user's notification preferences")
    fun updatePreferences(
        @Valid @RequestBody request: UpdatePreferenceRequest
    ): ResponseEntity<PreferenceResponse> {
        val userId = UUID.fromString(
            SecurityUtils.getCurrentUserId() ?: return ResponseEntity.status(401).build()
        )
        val tenantId = SecurityUtils.getCurrentTenantIdAsUUID()
            ?: return ResponseEntity.status(400).build()

        val preference = preferenceRepository.findByUserId(userId).orElseGet {
            NotificationPreference(
                userId = userId,
                tenantId = tenantId,
                email = request.email ?: ""
            )
        }

        preference.emailEnabled = request.emailEnabled
        preference.pushEnabled = request.pushEnabled
        preference.fcmToken = request.fcmToken
        request.email?.let { preference.email = it }
        preference.updatedAt = Instant.now()

        val saved = preferenceRepository.save(preference)
        return ResponseEntity.ok(saved.toResponse())
    }

    private fun NotificationPreference.toResponse() = PreferenceResponse(
        id = id,
        userId = userId,
        emailEnabled = emailEnabled,
        pushEnabled = pushEnabled,
        fcmToken = fcmToken,
        email = email,
        updatedAt = updatedAt
    )
}
