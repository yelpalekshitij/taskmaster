package com.taskmaster.task.comment

import com.taskmaster.task.comment.dto.TaskCommentDto
import com.taskmaster.task.common.TenantContext
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class TaskCommentResolver(private val taskCommentService: TaskCommentService) {

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun addComment(
        @Argument taskId: String,
        @Argument content: String,
        @AuthenticationPrincipal jwt: Jwt
    ): TaskCommentDto {
        val userId = UUID.fromString(jwt.subject)
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return taskCommentService.addComment(UUID.fromString(taskId), tenantId, userId, content)
    }
}
