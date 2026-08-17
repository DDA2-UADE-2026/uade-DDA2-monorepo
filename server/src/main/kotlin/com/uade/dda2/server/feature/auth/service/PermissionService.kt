package com.uade.dda2.server.feature.auth.service

import com.uade.dda2.server.feature.auth.dto.response.PermissionResponse
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PermissionService(
    private val permissionRepository: PermissionRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<PermissionResponse> =
        permissionRepository.findAllByOrderByNameAsc()
            .map {
                PermissionResponse(
                    id = requireNotNull(it.id),
                    name = it.name,
                )
            }
}
