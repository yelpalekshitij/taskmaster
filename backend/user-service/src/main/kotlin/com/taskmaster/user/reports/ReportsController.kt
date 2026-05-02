package com.taskmaster.user.reports

import com.taskmaster.user.tenant.TenantRepository
import com.taskmaster.user.user.UserRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class GlobalStatsDto(
    val totalTenants: Long,
    val activeTenants: Long,
    val totalUsers: Long,
    val activeUsers: Long,
    val totalTasks: Long = 0,
    val completedTasks: Long = 0
)

data class TenantStatsDto(
    val tenantId: String,
    val tenantName: String,
    val totalUsers: Long,
    val activeUsers: Long,
    val totalTasks: Long = 0,
    val completedTasks: Long = 0,
    val inProgressTasks: Long = 0,
    val overdueTasks: Long = 0,
    val taskCompletionRate: Double = 0.0
)

@RestController
@RequestMapping("/api/v1/reports")
class ReportsController(
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository
) {

    @GetMapping("/global")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    fun getGlobalStats(): GlobalStatsDto = GlobalStatsDto(
        totalTenants = tenantRepository.count(),
        activeTenants = tenantRepository.countByActive(true),
        totalUsers = userRepository.count(),
        activeUsers = userRepository.countByActive(true)
    )

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('MASTER_ADMIN') or hasRole('TENANT_ADMIN')")
    fun getTenantStats(): List<TenantStatsDto> =
        tenantRepository.findAll().map { tenant ->
            TenantStatsDto(
                tenantId = tenant.id.toString(),
                tenantName = tenant.name,
                totalUsers = userRepository.countByTenantId(tenant.id),
                activeUsers = userRepository.countByTenantIdAndActive(tenant.id, true)
            )
        }
}
