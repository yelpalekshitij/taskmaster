package com.taskmaster.task.common

object TenantContext {
    private val tenantIdHolder = ThreadLocal<String?>()

    fun get(): String? = tenantIdHolder.get()
    fun set(id: String?) = tenantIdHolder.set(id)
    fun clear() = tenantIdHolder.remove()

    fun getRequired(): String = get()
        ?: throw IllegalStateException("No tenant context available")
}
