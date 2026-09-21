package com.uade.dda2.server.feature.appointment.service

import com.uade.dda2.server.config.AppointmentProperties
import com.uade.dda2.server.feature.appointment.dto.response.AppointmentCenterResponse
import com.uade.dda2.server.feature.appointment.dto.response.AppointmentServiceResponse
import com.uade.dda2.server.feature.appointment.dto.response.AvailableAppointmentSlotResponse
import com.uade.dda2.server.feature.appointment.error.AppointmentErrors
import com.uade.dda2.server.feature.appointment.repository.AppointmentRepository
import com.uade.dda2.server.feature.appointment.validator.AppointmentValidator
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.center.repository.CenterServiceRepository
import com.uade.dda2.server.feature.center.repository.MunicipalServiceRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAvailabilityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AppointmentSlotService(
    private val services: MunicipalServiceRepository,
    private val centerServices: CenterServiceRepository,
    private val availabilities: ProfessionalAvailabilityRepository,
    private val appointments: AppointmentRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: AppointmentValidator,
    private val properties: AppointmentProperties,
    private val generator: AppointmentSlotGenerator,
) {
    @Transactional(readOnly = true)
    fun listServices(): List<AppointmentServiceResponse> {
        authorizedCitizen("appointments:own:view")
        return services.findEffectiveForAppointments().map {
            AppointmentServiceResponse(
                id = requireNotNull(it.id),
                name = it.name,
                description = it.description,
                durationMinutes = it.durationMinutes,
            )
        }
    }

    @Transactional(readOnly = true)
    fun listCenters(serviceId: UUID): List<AppointmentCenterResponse> {
        authorizedCitizen("appointments:own:view")
        val service = services.findById(serviceId).orElseThrow(AppointmentErrors::resourceNotFound)
        if (!service.active) throw AppointmentErrors.resourceNotFound()
        return centerServices.findEffectiveByServiceId(serviceId).map {
            AppointmentCenterResponse(
                centerServiceId = requireNotNull(it.id),
                centerId = requireNotNull(it.center.id),
                name = it.center.name,
                address = it.center.address,
                phone = it.center.phone,
                email = it.center.email,
            )
        }
    }

    @Transactional(readOnly = true)
    fun listSlots(centerServiceId: UUID, date: LocalDate): List<AvailableAppointmentSlotResponse> {
        val citizen = authorizedCitizen("appointments:own:view")
        return availableSlots(centerServiceId, date, requireNotNull(citizen.id), OffsetDateTime.now(properties.zone()))
    }

    fun availableSlots(
        centerServiceId: UUID,
        date: LocalDate,
        citizenId: Long,
        now: OffsetDateTime,
        includeOccupancy: Boolean = true,
    ): List<AvailableAppointmentSlotResponse> {
        validator.validateDate(date, now, properties.zone())
        val centerService = centerServices.findById(centerServiceId).orElseThrow(AppointmentErrors::resourceNotFound)
        if (!centerService.active || !centerService.center.active || !centerService.service.active) {
            throw AppointmentErrors.resourceNotFound()
        }
        val effectiveAvailabilities = availabilities.findEffectiveByCenterServiceIdAndDayOfWeek(
            centerServiceId,
            date.dayOfWeek,
        )
        if (effectiveAvailabilities.isEmpty()) return emptyList()
        val dayStart = date.atStartOfDay(properties.zone()).toOffsetDateTime()
        val dayEnd = date.plusDays(1).atStartOfDay(properties.zone()).toOffsetDateTime()
        val professionalIds = effectiveAvailabilities.map { requireNotNull(it.assignment.professional.id) }.distinct()
        val professionalAppointments = if (includeOccupancy) {
            appointments.findByProfessionalIdsInRange(professionalIds, dayStart, dayEnd)
        } else {
            emptyList()
        }
        val citizenAppointments = if (includeOccupancy) {
            appointments.findByCitizenIdInRange(citizenId, dayStart, dayEnd)
        } else {
            emptyList()
        }
        return generator.generate(
            date = date,
            durationMinutes = centerService.service.durationMinutes,
            availabilities = effectiveAvailabilities,
            professionalAppointments = professionalAppointments,
            citizenAppointments = citizenAppointments,
            zone = properties.zone(),
            now = now,
        )
    }

    private fun authorizedCitizen(permission: String): User {
        val principal = currentUser.principal()
        return validator.validateCitizen(users.findByIdWithRoles(principal.id), principal, permission)
    }
}
