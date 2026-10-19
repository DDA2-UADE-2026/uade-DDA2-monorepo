package com.uade.dda2.server.feature.benefit.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException

object BenefitGrantErrors {
    fun notFound() = NotFoundException("BENEFIT_GRANT_NOT_FOUND", "Otorgamiento inexistente o ajeno a la solicitud.")
    fun invalidTransition() = ConflictException("BENEFIT_GRANT_INVALID_TRANSITION", "El estado del otorgamiento no admite esta operacion.")
    fun requirementsNotMet() = ConflictException("BENEFIT_GRANT_REQUIREMENTS_NOT_MET", "Todos los requisitos vigentes deben estar cumplidos para renovar.")
    fun documentsNotValid() = ConflictException("BENEFIT_GRANT_DOCUMENTS_NOT_VALID", "Los documentos obligatorios deben estar en VALID para renovar.")
    fun inconsistentEnrollment() = ConflictException("BENEFIT_GRANT_INCONSISTENT_ENROLLMENT", "No se puede liberar el cupo: el contador de la edicion es inconsistente.")
    fun invalidData(message: String) = BadRequestException("BENEFIT_GRANT_INVALID_DATA", message)
    fun applicationNotApproved() = ConflictException("BENEFIT_GRANT_APPLICATION_NOT_APPROVED", "La solicitud debe estar aprobada.")
    fun editionNotActive() = ConflictException("BENEFIT_GRANT_EDITION_NOT_ACTIVE", "La edicion debe estar activa para otorgar o renovar beneficios.")
    fun alreadyExists() = ConflictException("BENEFIT_GRANT_ALREADY_EXISTS", "El beneficio ya fue otorgado a esta solicitud.")
    fun incompatibleApplication() = ConflictException("BENEFIT_GRANT_INCOMPATIBLE_APPLICATION", "El titular tiene una aprobacion incompatible con vigencia superpuesta.")
}
