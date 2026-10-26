package com.uade.dda2.server.feature.intervention.mapper

import com.uade.dda2.server.feature.intervention.entity.InterventionActionStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InterventionProgressTest {
    @Test fun `estado contempla pendientes avances y finalizaciones directas parciales`() {
        assertEquals(InterventionActionStatus.PENDING, interventionProgress(3, 3, 0))
        assertEquals(InterventionActionStatus.COMPLETED, interventionProgress(3, 0, 3))
        assertEquals(InterventionActionStatus.IN_PROGRESS, interventionProgress(3, 2, 1))
        assertEquals(InterventionActionStatus.IN_PROGRESS, interventionProgress(3, 0, 0))
        assertEquals(InterventionActionStatus.IN_PROGRESS, interventionProgress(3, 1, 0))
        assertEquals(InterventionActionStatus.PENDING, interventionProgress(0, 0, 0))
    }
}
