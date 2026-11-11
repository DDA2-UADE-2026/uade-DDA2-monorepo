package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreOutboxLease
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.messaging.CoreAmqpPublisher
import com.uade.dda2.server.feature.integration.service.CoreOutboxPublisher
import com.uade.dda2.server.feature.integration.service.CoreOutboxStore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.mockito.Mockito.*
import java.util.UUID

class CoreOutboxPublisherBranchTest {
    private class Fixture {
        val store = mock(CoreOutboxStore::class.java)
        val sender = mock(CoreAmqpPublisher::class.java)
        val properties = CoreProperties().apply { publisher.batchSize = 2 }
        val publisher = CoreOutboxPublisher(store, sender, properties)
        val lease = CoreOutboxLease(UUID.randomUUID(), UUID.randomUUID(), "{}", "qa-trace")
    }

    @TestFactory
    fun `publication attempts confirmation whether lease is current or stale`() = listOf(true, false).map { confirmed -> dynamicTest("confirmed=$confirmed") {
        val f = Fixture()
        `when`(f.store.reserveNext()).thenReturn(f.lease, null)
        `when`(f.store.confirm(f.lease)).thenReturn(confirmed)
        f.publisher.publishPending()
        val order = inOrder(f.store, f.sender)
        order.verify(f.store).reserveNext()
        order.verify(f.sender).publish(f.lease)
        order.verify(f.store).confirm(f.lease)
        order.verify(f.store).reserveNext()
        verifyNoMoreInteractions(f.store, f.sender)
    } }

    @TestFactory
    fun `failed publication is retried using safe error code`() = listOf(
        CoreIntegrationException("CORE_BROKER_UNAVAILABLE") to "CORE_BROKER_UNAVAILABLE",
        IllegalStateException("private details") to "CORE_PUBLISH_UNAVAILABLE",
    ).map { (failure, code) -> dynamicTest(code) {
        val f = Fixture()
        `when`(f.store.reserveNext()).thenReturn(f.lease, null)
        doThrow(failure).`when`(f.sender).publish(f.lease)
        f.publisher.publishPending()
        verify(f.store).retry(f.lease, code)
        verify(f.store, never()).confirm(f.lease)
    } }

    @Test
    fun `batch limit bounds work and an empty queue does not publish`() {
        val f = Fixture()
        f.publisher.publishPending()
        verifyNoInteractions(f.sender)
        clearInvocations(f.store)
        `when`(f.store.reserveNext()).thenReturn(f.lease)
        f.publisher.publishPending()
        verify(f.sender, times(2)).publish(f.lease)
        verify(f.store, times(2)).reserveNext()
    }

    @Test
    fun `polling failures are contained and interruption stops before reserving`() {
        val f = Fixture()
        `when`(f.store.reserveNext()).thenThrow(IllegalStateException("database unavailable"))
        f.publisher.publishPending()
        verifyNoInteractions(f.sender)
        clearInvocations(f.store)
        try {
            Thread.currentThread().interrupt()
            f.publisher.publishPending()
            verifyNoInteractions(f.store)
        } finally { Thread.interrupted() }
    }
}
