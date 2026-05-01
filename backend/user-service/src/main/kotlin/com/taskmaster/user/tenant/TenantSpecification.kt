package com.taskmaster.user.tenant

import org.springframework.data.jpa.domain.Specification

object TenantSpecification {

    fun nameContains(name: String?): Specification<Tenant>? {
        if (name.isNullOrBlank()) return null
        return Specification { root, _, cb ->
            cb.like(cb.lower(root.get("name")), "%${name.lowercase()}%")
        }
    }

    fun isActive(active: Boolean?): Specification<Tenant>? {
        if (active == null) return null
        return Specification { root, _, cb ->
            cb.equal(root.get<Boolean>("active"), active)
        }
    }

    fun domainContains(domain: String?): Specification<Tenant>? {
        if (domain.isNullOrBlank()) return null
        return Specification { root, _, cb ->
            cb.like(cb.lower(root.get("domain")), "%${domain.lowercase()}%")
        }
    }

    fun buildSpec(name: String?, active: Boolean?, domain: String?): Specification<Tenant> {
        var spec: Specification<Tenant> = Specification.where(null)
        nameContains(name)?.let { spec = spec.and(it) }
        isActive(active)?.let { spec = spec.and(it) }
        domainContains(domain)?.let { spec = spec.and(it) }
        return spec
    }
}
