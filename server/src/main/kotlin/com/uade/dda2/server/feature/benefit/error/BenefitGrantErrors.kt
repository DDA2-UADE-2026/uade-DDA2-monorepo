package com.uade.dda2.server.feature.benefit.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException

object BenefitGrantErrors {
    fun invalidData(message: String) = BadRequestException("BENEFIT_GRANT_INVALID_DATA", message)
    fun applicationNotApproved() = ConflictException("BENEFIT_GRANT_APPLICATION_NOT_APPROVED", "La solicitud debe estar aprobada.")
    fun editionNotActive() = ConflictException("BENEFIT_GRANT_EDITION_NOT_ACTIVE", "La edicion debe estar activa para otorgar beneficios.")
    fun alreadyExists() = ConflictException("BENEFIT_GRANT_ALREADY_EXISTS", "El beneficio ya fue otorgado a esta solicitud.")
    fun incompatibleApplication() = ConflictException("BENEFIT_GRANT_INCOMPATIBLE_APPLICATION", "El titular tiene una aprobacion incompatible con vigencia superpuesta.")
}
