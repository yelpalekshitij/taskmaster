package com.taskmaster.user.user

import com.taskmaster.user.common.SecurityUtils
import com.taskmaster.user.common.TenantContext
import com.taskmaster.user.keycloak.KeycloakAdminService
import com.taskmaster.user.role.RoleRepository
import com.taskmaster.user.role.UserRole
import com.taskmaster.user.role.UserRoleId
import com.taskmaster.user.role.UserRoleRepository
import com.taskmaster.user.user.dto.*
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userRoleRepository: UserRoleRepository,
    private val keycloakAdminService: KeycloakAdminService
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    @Transactional(readOnly = true)
    fun findByTenant(tenantId: UUID, pageable: Pageable): Page<UserDto> {
        return userRepository.findByTenantId(tenantId, pageable).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    @Cacheable("users", key = "#id")
    fun findById(id: UUID): UserDto {
        return userRepository.findById(id)
            .orElseThrow { NoSuchElementException("User not found: $id") }
            .toDto()
    }

    @Transactional(readOnly = true)
    fun findCurrentUser(): UserDto {
        val keycloakId = SecurityUtils.getCurrentUserId()
            ?: throw IllegalStateException("No authenticated user")
        return userRepository.findByKeycloakId(keycloakId)
            .orElseThrow { NoSuchElementException("User not found for keycloakId: $keycloakId") }
            .toDto()
    }

    fun create(tenantId: UUID, request: CreateUserRequest): UserDto {
        require(!userRepository.existsByEmailAndTenantId(request.email, tenantId)) {
            "User with email '${request.email}' already exists in this tenant"
        }

        val keycloakId = keycloakAdminService.createAppUser(
            tenantId = tenantId,
            email = request.email,
            username = request.username,
            firstName = request.firstName,
            lastName = request.lastName,
            password = request.password
        )

        val user = User(
            keycloakId = keycloakId,
            email = request.email,
            username = request.username,
            firstName = request.firstName,
            lastName = request.lastName,
            tenantId = tenantId
        )
        val saved = userRepository.save(user)

        if (request.roleIds.isNotEmpty()) {
            assignRoles(saved.id, request.roleIds)
        }

        log.info("User created: id={}, tenantId={}", saved.id, tenantId)
        return saved.toDto()
    }

    @CacheEvict("users", key = "#id")
    fun update(id: UUID, request: UpdateUserRequest): UserDto {
        val user = userRepository.findById(id)
            .orElseThrow { NoSuchElementException("User not found: $id") }

        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }
        request.active?.let { user.active = it }

        request.roleIds?.let { newRoleIds ->
            userRoleRepository.deleteByUserId(id)
            assignRoles(id, newRoleIds)
        }

        return userRepository.save(user).toDto()
    }

    private fun assignRoles(userId: UUID, roleIds: List<UUID>) {
        roleIds.forEach { roleId ->
            if (roleRepository.existsById(roleId)) {
                userRoleRepository.save(UserRole(UserRoleId(userId, roleId)))
            }
        }
    }

    private fun User.toDto() = UserDto(
        id = id,
        keycloakId = keycloakId,
        email = email,
        username = username,
        firstName = firstName,
        lastName = lastName,
        tenantId = tenantId,
        active = active,
        createdAt = createdAt
    )
}
