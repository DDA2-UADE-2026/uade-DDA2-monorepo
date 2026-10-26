package com.uade.dda2.server.feature.benefit.scheduler

import com.uade.dda2.server.config.BenefitGrantExpirationProperties
import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.benefit.service.BenefitGrantExpirationService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.data.domain.PageRequest
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.*
import java.util.UUID
import java.util.concurrent.*
import kotlin.test.*

class BenefitGrantExpirationSchedulerTest {
    private val grants = mock(BenefitGrantRepository::class.java)
    private val service = mock(BenefitGrantExpirationService::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-11-01T02:30:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 31)
    private val states = BenefitGrantExpirationService.EXPIRABLE_STATUSES
    private val page = PageRequest.of(0, 1)
    private fun scheduler() = BenefitGrantExpirationScheduler(grants, service,
        BenefitGrantExpirationProperties(batchSize = 1), EnrollmentPeriodExpirationProperties(), clock)

    @Test fun `cursor recorre todos los lotes aunque cambien los anteriores y reintenta fallidos al reiniciar`() {
        val first = UUID.randomUUID(); val second = UUID.randomUUID()
        `when`(grants.findExpirationCandidates(states, today, null, page)).thenReturn(listOf(first))
        `when`(grants.findExpirationCandidates(states, today, first, page)).thenReturn(listOf(second))
        `when`(grants.findExpirationCandidates(states, today, second, page)).thenReturn(emptyList())
        `when`(service.expireApplication(first)).thenThrow(IllegalStateException("error no publicable")).thenReturn(1)
        `when`(service.expireApplication(second)).thenReturn(2)
        val scheduler = scheduler()
        scheduler.expireBenefits()
        verify(service).expireApplication(second)
        scheduler.expireBenefits()
        verify(service, times(2)).expireApplication(first)
        verify(service, times(2)).expireApplication(second)
    }

    @Test fun `evita solapamiento local y libera guardia al terminar`() {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        `when`(grants.findExpirationCandidates(states, today, null, page)).thenAnswer {
            entered.countDown(); assertTrue(release.await(5, TimeUnit.SECONDS)); emptyList<UUID>()
        }
        val scheduler = scheduler()
        val pool = Executors.newSingleThreadExecutor()
        try {
            val first = pool.submit { scheduler.expireBenefits() }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            scheduler.expireBenefits()
            verify(grants, times(1)).findExpirationCandidates(states, today, null, page)
            release.countDown()
            first.get(5, TimeUnit.SECONDS)
            scheduler.expireBenefits()
            verify(grants, times(2)).findExpirationCandidates(states, today, null, page)
        } finally { release.countDown(); pool.shutdownNow() }
    }

    @Test fun `recupera guardia despues de un fallo de descubrimiento`() {
        `when`(grants.findExpirationCandidates(states, today, null, page)).thenThrow(IllegalStateException()).thenReturn(emptyList())
        val scheduler = scheduler()
        assertFailsWith<IllegalStateException> { scheduler.expireBenefits() }
        scheduler.expireBenefits()
        verify(grants, times(2)).findExpirationCandidates(states, today, null, page)
        verifyNoInteractions(service)
    }

    @Test fun `arranque procesa atrasados y configuracion deshabilitada no registra scheduler`() {
        `when`(grants.findExpirationCandidates(states, today, null, PageRequest.of(0, 100))).thenReturn(emptyList())
        val runner = ApplicationContextRunner().withUserConfiguration(BenefitGrantExpirationScheduler::class.java)
            .withBean(BenefitGrantRepository::class.java, { grants })
            .withBean(BenefitGrantExpirationService::class.java, { service })
            .withBean(BenefitGrantExpirationProperties::class.java, { BenefitGrantExpirationProperties() })
            .withBean(EnrollmentPeriodExpirationProperties::class.java, { EnrollmentPeriodExpirationProperties() })
            .withBean(Clock::class.java, { clock })
        runner.withPropertyValues("app.benefit-grant.expiration.enabled=false").run { context ->
            assertTrue(context.getBeansOfType(BenefitGrantExpirationScheduler::class.java).isEmpty())
        }
        verifyNoInteractions(grants)
        runner.withPropertyValues("app.benefit-grant.expiration.enabled=true").run { context ->
            context.publishEvent(ApplicationReadyEvent(SpringApplication(), emptyArray(), context.sourceApplicationContext, Duration.ZERO))
        }
        verify(grants).findExpirationCandidates(states, today, null, PageRequest.of(0, 100))
    }
}
