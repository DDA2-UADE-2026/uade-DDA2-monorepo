package com.uade.dda2.server.feature.center.error

import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import java.util.UUID

object CenterErrors {
    fun centerNotFound(id: UUID): NotFoundException =
        NotFoundException("MUNICIPAL_CENTER_NOT_FOUND", "No se encontró el centro municipal con id '$id'.")

    fun centerNameAlreadyExists(): ConflictException =
        ConflictException("MUNICIPAL_CENTER_NAME_ALREADY_EXISTS", "Ya existe un centro municipal con ese nombre.")

    fun centerAlreadyActive(): ConflictException =
        ConflictException("MUNICIPAL_CENTER_ALREADY_ACTIVE", "El centro municipal ya se encuentra activo.")

    fun centerAlreadyInactive(): ConflictException =
        ConflictException("MUNICIPAL_CENTER_ALREADY_INACTIVE", "El centro municipal ya se encuentra inactivo.")

    fun serviceNotFound(id: UUID): NotFoundException =
        NotFoundException("MUNICIPAL_SERVICE_NOT_FOUND", "No se encontró el servicio municipal con id '$id'.")

    fun serviceNameAlreadyExists(): ConflictException =
        ConflictException("MUNICIPAL_SERVICE_NAME_ALREADY_EXISTS", "Ya existe un servicio municipal con ese nombre.")

    fun serviceAlreadyActive(): ConflictException =
        ConflictException("MUNICIPAL_SERVICE_ALREADY_ACTIVE", "El servicio municipal ya se encuentra activo.")

    fun serviceAlreadyInactive(): ConflictException =
        ConflictException("MUNICIPAL_SERVICE_ALREADY_INACTIVE", "El servicio municipal ya se encuentra inactivo.")

    fun centerServiceNotFound(id: UUID): NotFoundException =
        NotFoundException("CENTER_SERVICE_NOT_FOUND", "No se encontró el servicio del centro con id '$id'.")

    fun centerServiceAlreadyActive(): ConflictException =
        ConflictException("CENTER_SERVICE_ALREADY_ACTIVE", "El servicio ya se encuentra asignado al centro.")

    fun centerServiceAlreadyInactive(): ConflictException =
        ConflictException("CENTER_SERVICE_ALREADY_INACTIVE", "El servicio del centro ya se encuentra inactivo.")

    fun inactiveDependency(name: String): ConflictException =
        ConflictException("CENTER_DEPENDENCY_INACTIVE", "No se puede completar la operación porque $name está inactivo.")
}
