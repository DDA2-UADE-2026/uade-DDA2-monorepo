package com.uade.dda2.server.feature.center.mapper

import com.uade.dda2.server.feature.center.dto.request.CreateMunicipalCenterRequest
import com.uade.dda2.server.feature.center.dto.request.UpdateMunicipalCenterRequest
import com.uade.dda2.server.feature.center.dto.response.MunicipalCenterListResponse
import com.uade.dda2.server.feature.center.dto.response.MunicipalCenterResponse
import com.uade.dda2.server.feature.center.entity.MunicipalCenter
import org.springframework.data.domain.Page

fun CreateMunicipalCenterRequest.toEntity(): MunicipalCenter =
    MunicipalCenter(name = name, address = address, phone = phone, email = email)

fun MunicipalCenter.updateFrom(request: UpdateMunicipalCenterRequest) {
    name = request.name
    address = request.address
    phone = request.phone
    email = request.email
}

fun MunicipalCenter.toResponse(): MunicipalCenterResponse =
    MunicipalCenterResponse(
        id = requireNotNull(id),
        name = name,
        address = address,
        phone = phone,
        email = email,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Page<MunicipalCenter>.toListResponse(): MunicipalCenterListResponse =
    MunicipalCenterListResponse(content.map(MunicipalCenter::toResponse), number, size, totalElements, totalPages)

fun MunicipalCenter.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "name" to name,
        "address" to address,
        "phone" to phone,
        "email" to email,
        "active" to active,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )
