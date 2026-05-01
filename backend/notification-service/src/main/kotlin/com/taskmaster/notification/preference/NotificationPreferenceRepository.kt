package com.taskmaster.notification.preference

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, UUID> {

    fun findByUserId(userId: UUID): Optional<NotificationPreference>
}
