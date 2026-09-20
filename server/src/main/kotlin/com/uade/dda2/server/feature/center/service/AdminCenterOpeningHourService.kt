package com.uade.dda2.server.feature.center.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.center.dto.request.CreateCenterOpeningHourRequest
import com.uade.dda2.server.feature.center.dto.request.CreateCenterOpeningHoursRequest
import com.uade.dda2.server.feature.center.dto.request.UpdateCenterOpeningHourRequest
import com.uade.dda2.server.feature.center.dto.response.CenterOpeningHourResponse
import com.uade.dda2.server.feature.center.entity.CenterOpeningHour
import com.uade.dda2.server.feature.center.error.CenterErrors
import com.uade.dda2.server.feature.center.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.center.mapper.toResponse
import com.uade.dda2.server.feature.center.mapper.updateFrom
import com.uade.dda2.server.feature.center.repository.CenterOpeningHourRepository
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.validator.CenterOpeningHourValidator
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.DayOfWeek
import java.util.UUID

@Service
class AdminCenterOpeningHourService(
    private val centerRepository: MunicipalCenterRepository,
    private val openingHourRepository: CenterOpeningHourRepository,
    private val validator: CenterOpeningHourValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun list(centerId: UUID): List<CenterOpeningHourResponse> {
        if (!centerRepository.existsById(centerId)) throw CenterErrors.centerNotFound(centerId)
        return openingHourRepository.findAllByCenterIdOrderByDayOfWeekAscStartTimeAsc(centerId)
            .map(CenterOpeningHour::toResponse)
    }

    @Transactional
    fun create(centerId: UUID, request: CreateCenterOpeningHourRequest): CenterOpeningHourResponse {
        val center = centerRepository.findByIdForUpdate(centerId) ?: throw CenterErrors.centerNotFound(centerId)
        if (!center.active) throw CenterErrors.inactiveDependency("el centro municipal")
        validator.validateRange(request.startTime, request.endTime)
        validator.validateNoOverlap(centerId, request.dayOfWeek, request.startTime, request.endTime)
        val openingHour = openingHourRepository.saveAndFlush(
            CenterOpeningHour(
                center = center,
                dayOfWeek = request.dayOfWeek,
                startTime = request.startTime,
                endTime = request.endTime,
            ),
        )
        record(openingHour, LogAction.CREATE)
        return openingHour.toResponse()
    }

    @Transactional
    fun createBulk(centerId: UUID, request: CreateCenterOpeningHoursRequest): List<CenterOpeningHourResponse> {
        val center = centerRepository.findByIdForUpdate(centerId) ?: throw CenterErrors.centerNotFound(centerId)
        if (!center.active) throw CenterErrors.inactiveDependency("el centro municipal")
        val days = request.days.distinct()
        if (days.isEmpty() || days.size != request.days.size) throw CenterErrors.invalidBulkDays()
        validator.validateRange(request.startTime, request.endTime)
        // Crear solo agrega cobertura, así que basta validar superposiciones por día.
        // Si un día falla, la transacción hace rollback y no se crea ninguna franja.
        days.forEach { day ->
            try {
                validator.validateNoOverlap(centerId, day, request.startTime, request.endTime)
            } catch (_: ConflictException) {
                throw CenterErrors.openingHourOverlapOnDay(day)
            }
        }
        return days.map { day ->
            val openingHour = openingHourRepository.saveAndFlush(
                CenterOpeningHour(
                    center = center,
                    dayOfWeek = day,
                    startTime = request.startTime,
                    endTime = request.endTime,
                ),
            )
            record(openingHour, LogAction.CREATE)
            openingHour.toResponse()
        }
    }

    @Transactional
    fun update(id: UUID, request: UpdateCenterOpeningHourRequest): CenterOpeningHourResponse {
        val openingHour = lockOpeningHour(id)
        val centerId = requireNotNull(openingHour.center.id)
        validator.validateRange(request.startTime, request.endTime)
        if (openingHour.active) {
            validator.validateNoOverlap(centerId, request.dayOfWeek, request.startTime, request.endTime, id)
            validateProjectedCoverage(openingHour, request.dayOfWeek, request)
        }
        val oldValues = json(openingHour.toAuditSnapshot())
        openingHour.updateFrom(request)
        openingHourRepository.saveAndFlush(openingHour)
        record(openingHour, LogAction.UPDATE, oldValues)
        return openingHour.toResponse()
    }

    @Transactional
    fun activate(id: UUID): CenterOpeningHourResponse {
        val openingHour = lockOpeningHour(id)
        if (openingHour.active) throw CenterErrors.openingHourAlreadyActive()
        if (!openingHour.center.active) throw CenterErrors.inactiveDependency("el centro municipal")
        val centerId = requireNotNull(openingHour.center.id)
        validator.validateNoOverlap(
            centerId,
            openingHour.dayOfWeek,
            openingHour.startTime,
            openingHour.endTime,
            id,
        )
        val projected = projectedHours(centerId, openingHour, active = true)
        validator.validateAvailabilityCoverage(centerId, openingHour.dayOfWeek, projected)
        return changeStatus(openingHour, true)
    }

    @Transactional
    fun deactivate(id: UUID): CenterOpeningHourResponse {
        val openingHour = lockOpeningHour(id)
        if (!openingHour.active) throw CenterErrors.openingHourAlreadyInactive()
        val centerId = requireNotNull(openingHour.center.id)
        val projected = projectedHours(centerId, openingHour, active = false)
        validator.validateAvailabilityCoverage(centerId, openingHour.dayOfWeek, projected)
        return changeStatus(openingHour, false)
    }

    private fun validateProjectedCoverage(
        current: CenterOpeningHour,
        newDay: DayOfWeek,
        request: UpdateCenterOpeningHourRequest,
    ) {
        val centerId = requireNotNull(current.center.id)
        val all = openingHourRepository.findAllByCenterIdOrderByDayOfWeekAscStartTimeAsc(centerId)
        val projected = all.map { hour ->
            if (hour.id == current.id) {
                CenterOpeningHour(
                    id = hour.id,
                    center = hour.center,
                    dayOfWeek = request.dayOfWeek,
                    startTime = request.startTime,
                    endTime = request.endTime,
                    active = true,
                )
            } else {
                hour
            }
        }
        validator.validateAvailabilityCoverage(centerId, current.dayOfWeek, projected)
        if (newDay != current.dayOfWeek) validator.validateAvailabilityCoverage(centerId, newDay, projected)
    }

    private fun projectedHours(centerId: UUID, target: CenterOpeningHour, active: Boolean): List<CenterOpeningHour> =
        openingHourRepository.findAllByCenterIdOrderByDayOfWeekAscStartTimeAsc(centerId).map { hour ->
            if (hour.id == target.id) {
                CenterOpeningHour(
                    id = hour.id,
                    center = hour.center,
                    dayOfWeek = hour.dayOfWeek,
                    startTime = hour.startTime,
                    endTime = hour.endTime,
                    active = active,
                )
            } else {
                hour
            }
        }

    private fun lockOpeningHour(id: UUID): CenterOpeningHour {
        val preview = openingHourRepository.findById(id).orElseThrow { CenterErrors.openingHourNotFound(id) }
        val centerId = requireNotNull(preview.center.id)
        centerRepository.findByIdForUpdate(centerId) ?: throw CenterErrors.centerNotFound(centerId)
        return openingHourRepository.findByIdForUpdate(id) ?: throw CenterErrors.openingHourNotFound(id)
    }

    private fun changeStatus(openingHour: CenterOpeningHour, active: Boolean): CenterOpeningHourResponse {
        val oldValues = json(openingHour.toAuditSnapshot())
        openingHour.active = active
        openingHourRepository.saveAndFlush(openingHour)
        record(openingHour, LogAction.UPDATE, oldValues)
        return openingHour.toResponse()
    }

    private fun record(openingHour: CenterOpeningHour, action: LogAction, oldValues: String? = null) {
        logService.record(
            user = currentUserService.userReference(),
            action = action,
            entityType = LogEntityType.CENTER_OPENING_HOUR,
            entityId = requireNotNull(openingHour.id).toString(),
            oldValues = oldValues,
            newValues = json(openingHour.toAuditSnapshot()),
        )
    }

    private fun json(value: Any): String = requireNotNull(jsonMapper.writeValueAsString(value))
}
