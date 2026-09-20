package com.uade.dda2.server.feature.center.validator

import com.uade.dda2.server.feature.center.entity.MunicipalNameNormalizer
import com.uade.dda2.server.feature.center.error.CenterErrors
import com.uade.dda2.server.feature.center.repository.MunicipalServiceRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MunicipalServiceValidator(
    private val repository: MunicipalServiceRepository,
) {
    fun validateName(name: String, excludedId: UUID? = null) {
        val normalized = MunicipalNameNormalizer.normalize(name)
        val exists = if (excludedId == null) {
            repository.existsByNormalizedName(normalized)
        } else {
            repository.existsByNormalizedNameAndIdNot(normalized, excludedId)
        }
        if (exists) throw CenterErrors.serviceNameAlreadyExists()
    }
}
