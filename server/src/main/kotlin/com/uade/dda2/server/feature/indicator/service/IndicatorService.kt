package com.uade.dda2.server.feature.indicator.service

import com.uade.dda2.server.config.AppointmentProperties
import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.feature.activity.entity.ActivityAttendanceStatus
import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleRequestStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import com.uade.dda2.server.feature.application.validator.ApplicationValidator
import com.uade.dda2.server.feature.appointment.entity.AppointmentStatus
import com.uade.dda2.server.feature.appointment.entity.AttentionResult
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.indicator.dto.ActivityIndicators
import com.uade.dda2.server.feature.indicator.dto.ApplicationTrendPoint
import com.uade.dda2.server.feature.indicator.dto.AppointmentIndicators
import com.uade.dda2.server.feature.indicator.dto.AppointmentTrendPoint
import com.uade.dda2.server.feature.indicator.dto.AttentionIndicators
import com.uade.dda2.server.feature.indicator.dto.AwaitingApplication
import com.uade.dda2.server.feature.indicator.dto.DocumentCounts
import com.uade.dda2.server.feature.indicator.dto.EditionCapacity
import com.uade.dda2.server.feature.indicator.dto.EvaluationMatrixCell
import com.uade.dda2.server.feature.indicator.dto.IndicatorBucket
import com.uade.dda2.server.feature.indicator.dto.IndicatorsResponse
import com.uade.dda2.server.feature.indicator.dto.ProgramIndicator
import com.uade.dda2.server.feature.indicator.dto.RescheduleRequestCounts
import com.uade.dda2.server.feature.indicator.dto.ResolutionCounts
import com.uade.dda2.server.feature.indicator.dto.StageTimes
import com.uade.dda2.server.feature.indicator.dto.StatusCount
import com.uade.dda2.server.feature.indicator.dto.SummaryIndicators
import com.uade.dda2.server.feature.indicator.dto.VisitTrendPoint
import com.uade.dda2.server.feature.indicator.dto.WorkerLoad
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

const val INDICATORS_VIEW_PERMISSION = "indicators:view"

private const val MAX_RANGE_DAYS = 1100L
private const val DEFAULT_RANGE_DAYS = 90L
private const val WEEK_BUCKET_MAX_DAYS = 120L
private const val STALE_AFTER_DAYS = 14
private const val WORKLOAD_LIMIT = 10

// Solicitudes en curso: todavía las trabaja alguien (incluye la lista de espera, que sigue abierta).
private val ACTIVE_STATUSES = ApplicationStatus.EVALUATION_ALLOWED + ApplicationStatus.IN_VISIT + ApplicationStatus.WAITLISTED
private val AWAITING_EVALUATION_STATUSES = setOf(
    ApplicationStatus.SUBMITTED, ApplicationStatus.IN_VALIDATION, ApplicationStatus.PENDING_DOCUMENTATION,
)
private val RESOLVED_STATUSES = setOf(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED, ApplicationStatus.WAITLISTED)
private val CURRENT_EDITION_STATUSES = setOf(ProgramEditionStatus.ACTIVE, ProgramEditionStatus.SUSPENDED)

/** Período pedido, expresado en las dos representaciones de fecha que usan las entidades. */
private class Period(val from: LocalDate, val to: LocalDate, zone: ZoneId) {
    val start: LocalDateTime = from.atStartOfDay()
    val end: LocalDateTime = to.plusDays(1).atStartOfDay()
    val offsetStart: OffsetDateTime = start.atZone(zone).toOffsetDateTime()
    val offsetEnd: OffsetDateTime = end.atZone(zone).toOffsetDateTime()
    val days: Long = ChronoUnit.DAYS.between(from, to) + 1

    fun previous(zone: ZoneId) = Period(from.minusDays(days), from.minusDays(1), zone)
}

@Service
class IndicatorService(
    private val entityManager: EntityManager,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: ApplicationValidator,
    private val applicationTime: EnrollmentPeriodExpirationProperties,
    private val appointmentTime: AppointmentProperties,
) {
    @Transactional(readOnly = true)
    fun indicators(from: LocalDate?, to: LocalDate?, programId: UUID?): IndicatorsResponse {
        val principal = currentUser.principal()
        validator.validateUser(users.findByIdWithRoles(principal.id), principal, INDICATORS_VIEW_PERMISSION)

        val zone = applicationTime.zone()
        val today = LocalDate.now(zone)
        val end = to ?: today
        val start = from ?: end.minusDays(DEFAULT_RANGE_DAYS - 1)
        if (start.isAfter(end)) throw BadRequestException("INDICATORS_INVALID_RANGE", "La fecha de inicio no puede ser posterior a la de fin.")
        val period = Period(start, end, zone)
        if (period.days > MAX_RANGE_DAYS) {
            throw BadRequestException("INDICATORS_INVALID_RANGE", "El período no puede superar los $MAX_RANGE_DAYS días.")
        }
        val bucket = if (period.days <= WEEK_BUCKET_MAX_DAYS) IndicatorBucket.WEEK else IndicatorBucket.MONTH
        val buckets = buckets(period, bucket)
        val filter = ProgramFilter(programId)

        val cohort = cohort(period, filter)
        val resolved = resolvedInPeriod(period, filter, zone)
        val capacity = capacity(filter)
        val visits = visitRows(period, filter)
        val realized = visits.count { it.status == ApplicationVisitStatus.REALIZED }.toLong()

        val currentCapacity = capacity.filter { it.status in CURRENT_EDITION_STATUSES }
        val capacityTotal = currentCapacity.sumOf { it.maxCapacity.toLong() }
        val approvedTotal = currentCapacity.sumOf { it.approved }
        val approved = resolved.count { it.status == ApplicationStatus.APPROVED }.toLong()
        val rejected = resolved.count { it.status == ApplicationStatus.REJECTED }.toLong()
        val stageTimes = stageTimes(resolved)

        return IndicatorsResponse(
            from = period.from,
            to = period.to,
            programId = programId,
            generatedAt = OffsetDateTime.now(ZoneOffset.UTC),
            bucket = bucket,
            summary = SummaryIndicators(
                applicationsSubmitted = cohort.size.toLong(),
                previousApplicationsSubmitted = previousSubmitted(period.previous(zone), filter),
                resolved = resolved.size.toLong(),
                approvalRate = (approved + rejected).takeIf { it > 0 }?.let { approved.toDouble() / it },
                capacityUsedRate = capacityTotal.takeIf { it > 0 }?.let { approvedTotal.toDouble() / it },
                approvedTotal = approvedTotal,
                capacityTotal = capacityTotal,
                visitsRealized = realized,
                previousVisitsRealized = previousVisitsRealized(period.previous(zone), filter),
                averageDaysToResolution = stageTimes.averageDaysToResolution,
            ),
            applicationsTrend = buckets.map { bucketStart ->
                ApplicationTrendPoint(
                    periodStart = bucketStart,
                    submitted = cohort.count { bucketOf(it.submittedAt.toLocalDate(), bucket) == bucketStart }.toLong(),
                    resolved = resolved.count { bucketOf(it.resolvedAt.toLocalDate(), bucket) == bucketStart }.toLong(),
                )
            },
            statusDistribution = ApplicationStatus.entries
                .filter { it != ApplicationStatus.DRAFT }
                .map { status -> StatusCount(status, cohort.count { it.status == status }.toLong()) },
            resolutions = ResolutionCounts(
                approved = approved,
                waitlisted = resolved.count { it.status == ApplicationStatus.WAITLISTED }.toLong(),
                rejected = rejected,
            ),
            programs = cohort.groupBy { it.programId }.map { (id, rows) ->
                ProgramIndicator(
                    programId = id,
                    programName = rows.first().programName,
                    submitted = rows.size.toLong(),
                    inProgress = rows.count { it.status !in RESOLVED_STATUSES && it.status != ApplicationStatus.CLOSED }.toLong(),
                    approved = rows.count { it.status == ApplicationStatus.APPROVED }.toLong(),
                    waitlisted = rows.count { it.status == ApplicationStatus.WAITLISTED }.toLong(),
                    rejected = rows.count { it.status == ApplicationStatus.REJECTED }.toLong(),
                )
            }.sortedByDescending { it.submitted },
            capacity = capacity.sortedWith(
                compareByDescending<EditionCapacity> { it.status in CURRENT_EDITION_STATUSES }
                    .thenByDescending { if (it.maxCapacity > 0) it.approved.toDouble() / it.maxCapacity else 0.0 },
            ),
            evaluationMatrix = evaluationMatrix(period, filter),
            attention = attention(filter, today),
            visitsTrend = buckets.map { bucketStart ->
                val inBucket = visits.filter { bucketOf(it.startsAt.atZoneSameInstant(zone).toLocalDate(), bucket) == bucketStart }
                VisitTrendPoint(
                    periodStart = bucketStart,
                    scheduled = inBucket.count { it.status == ApplicationVisitStatus.SCHEDULED }.toLong(),
                    realized = inBucket.count { it.status == ApplicationVisitStatus.REALIZED }.toLong(),
                    notRealized = inBucket.count { it.status == ApplicationVisitStatus.NOT_REALIZED }.toLong(),
                    cancelled = inBucket.count { it.status == ApplicationVisitStatus.CANCELLED }.toLong(),
                )
            },
            rescheduleRequests = rescheduleRequests(period, filter),
            workload = workload(filter),
            stageTimes = stageTimes,
            documents = documents(period, filter),
            appointments = appointments(period, buckets, bucket),
            activities = activities(period),
        )
    }

    // ---- Solicitudes ----

    private data class CohortRow(
        val status: ApplicationStatus,
        val submittedAt: LocalDateTime,
        val programId: UUID,
        val programName: String,
    )

    private fun cohort(period: Period, filter: ProgramFilter): List<CohortRow> =
        query(
            "select a.status, a.submittedAt, p.id, p.name from Application a join a.programEdition e join e.program p " +
                "where a.submittedAt >= :start and a.submittedAt < :end",
            filter,
        ).setParameter("start", period.start).setParameter("end", period.end).resultList.map {
            CohortRow(it[0] as ApplicationStatus, it[1] as LocalDateTime, it[2] as UUID, it[3] as String)
        }

    private fun previousSubmitted(period: Period, filter: ProgramFilter): Long =
        count(
            "select count(a) from Application a join a.programEdition e join e.program p " +
                "where a.submittedAt >= :start and a.submittedAt < :end",
            filter,
        ).setParameter("start", period.start).setParameter("end", period.end).singleResult

    private data class ResolvedRow(
        val status: ApplicationStatus,
        val submittedAt: LocalDateTime,
        val resolvedAt: LocalDateTime,
        val evaluatedAt: LocalDateTime?,
    )

    private fun resolvedInPeriod(period: Period, filter: ProgramFilter, zone: ZoneId): List<ResolvedRow> =
        query(
            "select a.status, a.submittedAt, a.resolvedAt, ev.evaluatedAt from Application a " +
                "join a.programEdition e join e.program p left join ApplicationEvaluation ev on ev.application = a " +
                "where a.resolvedAt >= :start and a.resolvedAt < :end and a.status in :statuses",
            filter,
        ).setParameter("start", period.start).setParameter("end", period.end)
            .setParameter("statuses", RESOLVED_STATUSES).resultList.map {
                ResolvedRow(
                    it[0] as ApplicationStatus,
                    it[1] as LocalDateTime,
                    it[2] as LocalDateTime,
                    (it[3] as OffsetDateTime?)?.atZoneSameInstant(zone)?.toLocalDateTime(),
                )
            }

    private fun stageTimes(rows: List<ResolvedRow>): StageTimes {
        fun days(from: LocalDateTime, to: LocalDateTime) = Duration.between(from, to).toMinutes() / (60.0 * 24)
        val evaluated = rows.filter { it.evaluatedAt != null }
        return StageTimes(
            averageDaysToEvaluation = evaluated.map { days(it.submittedAt, it.evaluatedAt!!) }.averageOrNull(),
            averageDaysEvaluationToResolution = evaluated.map { days(it.evaluatedAt!!, it.resolvedAt) }.averageOrNull(),
            averageDaysToResolution = rows.map { days(it.submittedAt, it.resolvedAt) }.averageOrNull(),
            sampleSize = rows.size.toLong(),
        )
    }

    private fun capacity(filter: ProgramFilter): List<EditionCapacity> {
        val counts = entityManager.createQuery(
            "select a.programEdition.id, a.status, count(a) from Application a where a.status in :statuses " +
                "group by a.programEdition.id, a.status",
            Array<Any?>::class.java,
        ).setParameter("statuses", setOf(ApplicationStatus.APPROVED, ApplicationStatus.WAITLISTED)).resultList
            .associate { (it[0] as UUID to it[1] as ApplicationStatus) to it[2] as Long }
        return query(
            "select e.id, p.id, p.name, e.name, e.status, e.maxCapacity from ProgramEdition e join e.program p " +
                "where e.status <> :draft",
            filter,
        ).setParameter("draft", ProgramEditionStatus.DRAFT).resultList.map {
            val editionId = it[0] as UUID
            EditionCapacity(
                editionId = editionId,
                programId = it[1] as UUID,
                programName = it[2] as String,
                editionName = it[3] as String,
                status = it[4] as ProgramEditionStatus,
                maxCapacity = it[5] as Int,
                approved = counts[editionId to ApplicationStatus.APPROVED] ?: 0,
                waitlisted = counts[editionId to ApplicationStatus.WAITLISTED] ?: 0,
            )
        }
    }

    private fun evaluationMatrix(period: Period, filter: ProgramFilter): List<EvaluationMatrixCell> {
        val counts = query(
            "select ev.vulnerability, ev.priority, count(ev) from ApplicationEvaluation ev join ev.application a " +
                "join a.programEdition e join e.program p where ev.evaluatedAt >= :start and ev.evaluatedAt < :end",
            filter,
            suffix = " group by ev.vulnerability, ev.priority",
        ).setParameter("start", period.offsetStart).setParameter("end", period.offsetEnd).resultList
            .associate { (it[0] as VulnerabilityLevel to it[1] as EvaluationPriority) to it[2] as Long }
        // Grilla completa, con ceros, para que el mapa de calor no tenga huecos.
        return VulnerabilityLevel.entries.flatMap { vulnerability ->
            EvaluationPriority.entries.map { priority ->
                EvaluationMatrixCell(vulnerability, priority, counts[vulnerability to priority] ?: 0)
            }
        }
    }

    private fun attention(filter: ProgramFilter, today: LocalDate): AttentionIndicators {
        val staleBefore = today.minusDays(STALE_AFTER_DAYS.toLong()).atStartOfDay()
        val unassigned = count(
            "select count(a) from Application a join a.programEdition e join e.program p " +
                "where a.status in :statuses and a.assignedWorker is null",
            filter,
        ).setParameter("statuses", ACTIVE_STATUSES).singleResult
        val awaitingWhere = "from Application a join a.programEdition e join e.program p " +
            "where a.status in :statuses and a.submittedAt < :staleBefore " +
            "and not exists (select 1 from ApplicationEvaluation ev where ev.application = a)"
        val awaitingCount = count("select count(a) $awaitingWhere", filter)
            .setParameter("statuses", AWAITING_EVALUATION_STATUSES).setParameter("staleBefore", staleBefore).singleResult
        val oldest = query(
            "select a.id, a.applicationNumber, p.name, a.status, a.submittedAt $awaitingWhere",
            filter,
            suffix = " order by a.submittedAt asc",
        ).setParameter("statuses", AWAITING_EVALUATION_STATUSES).setParameter("staleBefore", staleBefore)
            .setMaxResults(5).resultList.map {
                val submittedAt = it[4] as LocalDateTime
                AwaitingApplication(
                    applicationId = it[0] as UUID,
                    applicationNumber = it[1] as Long,
                    programName = it[2] as String,
                    status = it[3] as ApplicationStatus,
                    submittedAt = submittedAt,
                    daysWaiting = ChronoUnit.DAYS.between(submittedAt.toLocalDate(), today),
                )
            }
        return AttentionIndicators(unassigned, awaitingCount, STALE_AFTER_DAYS, oldest)
    }

    // ---- Visitas ----

    private data class VisitRow(val status: ApplicationVisitStatus, val startsAt: OffsetDateTime)

    private fun visitRows(period: Period, filter: ProgramFilter): List<VisitRow> =
        query(
            "select v.status, v.startsAt from ApplicationVisit v join v.application a join a.programEdition e " +
                "join e.program p where v.startsAt >= :start and v.startsAt < :end",
            filter,
        ).setParameter("start", period.offsetStart).setParameter("end", period.offsetEnd).resultList.map {
            VisitRow(it[0] as ApplicationVisitStatus, it[1] as OffsetDateTime)
        }

    private fun previousVisitsRealized(period: Period, filter: ProgramFilter): Long =
        count(
            "select count(v) from ApplicationVisit v join v.application a join a.programEdition e join e.program p " +
                "where v.startsAt >= :start and v.startsAt < :end and v.status = :status",
            filter,
        ).setParameter("start", period.offsetStart).setParameter("end", period.offsetEnd)
            .setParameter("status", ApplicationVisitStatus.REALIZED).singleResult

    private fun rescheduleRequests(period: Period, filter: ProgramFilter): RescheduleRequestCounts {
        val counts = query(
            "select r.status, count(r) from ApplicationVisitRescheduleRequest r join r.visit v join v.application a " +
                "join a.programEdition e join e.program p where r.requestedAt >= :start and r.requestedAt < :end",
            filter,
            suffix = " group by r.status",
        ).setParameter("start", period.offsetStart).setParameter("end", period.offsetEnd).resultList
            .associate { it[0] as ApplicationVisitRescheduleRequestStatus to it[1] as Long }
        return RescheduleRequestCounts(
            pending = counts[ApplicationVisitRescheduleRequestStatus.PENDING] ?: 0,
            approved = counts[ApplicationVisitRescheduleRequestStatus.APPROVED] ?: 0,
            rejected = counts[ApplicationVisitRescheduleRequestStatus.REJECTED] ?: 0,
        )
    }

    private fun workload(filter: ProgramFilter): List<WorkerLoad> {
        val scheduled = query(
            "select v.responsible.id, count(v) from ApplicationVisit v join v.application a join a.programEdition e " +
                "join e.program p where v.status = :scheduled",
            filter,
            suffix = " group by v.responsible.id",
        ).setParameter("scheduled", ApplicationVisitStatus.SCHEDULED).resultList.associate { it[0] as Long to it[1] as Long }
        return query(
            "select w.id, w.name, count(a) from Application a join a.assignedWorker w join a.programEdition e " +
                "join e.program p where a.status in :statuses",
            filter,
            suffix = " group by w.id, w.name",
        ).setParameter("statuses", ACTIVE_STATUSES).resultList.map {
            val id = it[0] as Long
            WorkerLoad(userId = id, name = it[1] as String, activeCases = it[2] as Long, scheduledVisits = scheduled[id] ?: 0)
        }.sortedWith(compareByDescending<WorkerLoad> { it.activeCases }.thenBy { it.name }).take(WORKLOAD_LIMIT)
    }

    private fun documents(period: Period, filter: ProgramFilter): DocumentCounts {
        val counts = query(
            "select d.status, count(d) from ApplicationDocument d join d.application a join a.programEdition e " +
                "join e.program p where d.uploadedAt >= :start and d.uploadedAt < :end",
            filter,
            suffix = " group by d.status",
        ).setParameter("start", period.start).setParameter("end", period.end).resultList
            .associate { it[0] as ApplicationDocumentStatus to it[1] as Long }
        return DocumentCounts(
            pending = counts[ApplicationDocumentStatus.PENDING] ?: 0,
            valid = counts[ApplicationDocumentStatus.VALID] ?: 0,
            observed = counts[ApplicationDocumentStatus.OBSERVED] ?: 0,
        )
    }

    // ---- Turnos y actividades (no dependen del programa) ----

    private fun appointments(period: Period, buckets: List<LocalDate>, bucket: IndicatorBucket): AppointmentIndicators {
        val zone = appointmentTime.zone()
        val start = period.start.atZone(zone).toOffsetDateTime()
        val end = period.end.atZone(zone).toOffsetDateTime()
        val rows = entityManager.createQuery(
            "select ap.status, ap.startsAt from Appointment ap where ap.startsAt >= :start and ap.startsAt < :end",
            Array<Any?>::class.java,
        ).setParameter("start", start).setParameter("end", end).resultList
            .map { it[0] as AppointmentStatus to (it[1] as OffsetDateTime).atZoneSameInstant(zone).toLocalDate() }
        val results = entityManager.createQuery(
            "select c.result, count(c) from CommunityAttention c where c.appointment.startsAt >= :start " +
                "and c.appointment.startsAt < :end group by c.result",
            Array<Any?>::class.java,
        ).setParameter("start", start).setParameter("end", end).resultList.associate { it[0] as AttentionResult to it[1] as Long }
        return AppointmentIndicators(
            confirmed = rows.count { it.first == AppointmentStatus.CONFIRMED }.toLong(),
            cancelled = rows.count { it.first == AppointmentStatus.CANCELLED }.toLong(),
            attended = results[AttentionResult.ATENDIDO] ?: 0,
            absent = results[AttentionResult.AUSENTE] ?: 0,
            trend = buckets.map { bucketStart ->
                val inBucket = rows.filter { bucketOf(it.second, bucket) == bucketStart }
                AppointmentTrendPoint(
                    periodStart = bucketStart,
                    confirmed = inBucket.count { it.first == AppointmentStatus.CONFIRMED }.toLong(),
                    cancelled = inBucket.count { it.first == AppointmentStatus.CANCELLED }.toLong(),
                )
            },
        )
    }

    private fun activities(period: Period): ActivityIndicators {
        val counts = entityManager.createQuery(
            "select en.attendance, count(en) from ActivityEnrollment en where en.enrolledAt >= :start " +
                "and en.enrolledAt < :end group by en.attendance",
            Array<Any?>::class.java,
        ).setParameter("start", period.offsetStart).setParameter("end", period.offsetEnd).resultList
            .associate { it[0] as ActivityAttendanceStatus? to it[1] as Long }
        return ActivityIndicators(
            enrollments = counts.values.sum(),
            present = counts[ActivityAttendanceStatus.PRESENT] ?: 0,
            absent = counts[ActivityAttendanceStatus.ABSENT] ?: 0,
            unrecorded = counts[null] ?: 0,
        )
    }

    // ---- Utilidades ----

    /** Agrega el filtro de programa solo cuando se pidió, en lugar de un parámetro nulo comparado en SQL. */
    private class ProgramFilter(val programId: UUID?) {
        val clause: String get() = if (programId != null) " and p.id = :programId" else ""
        fun <T> apply(query: TypedQuery<T>): TypedQuery<T> =
            if (programId != null) query.setParameter("programId", programId) else query
    }

    private fun query(jpql: String, filter: ProgramFilter, suffix: String = ""): TypedQuery<Array<Any?>> =
        filter.apply(entityManager.createQuery(jpql + filter.clause + suffix, Array<Any?>::class.java))

    private fun count(jpql: String, filter: ProgramFilter): TypedQuery<Long> =
        filter.apply(entityManager.createQuery(jpql + filter.clause, Long::class.javaObjectType))

    private fun buckets(period: Period, bucket: IndicatorBucket): List<LocalDate> =
        generateSequence(bucketOf(period.from, bucket)) { if (bucket == IndicatorBucket.WEEK) it.plusWeeks(1) else it.plusMonths(1) }
            .takeWhile { !it.isAfter(period.to) }
            .toList()

    private fun bucketOf(date: LocalDate, bucket: IndicatorBucket): LocalDate =
        if (bucket == IndicatorBucket.WEEK) date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) else date.withDayOfMonth(1)

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
