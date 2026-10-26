package com.uade.dda2.server.feature.benefit.service

import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.auth.entity.*
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.benefit.entity.BenefitGrant
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.benefit.validator.BenefitGrantValidator
import com.uade.dda2.server.security.JwtPrincipal
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import kotlin.test.*

class CitizenBenefitGrantServiceTest {
    private val grants = mock(BenefitGrantRepository::class.java)
    private val users = mock(UserRepository::class.java)
    private val current = mock(CurrentUserService::class.java)
    private val role = Role(id = 3, name = "ROL_CONFIGURABLE", permissions = mutableSetOf(Permission(name = OWN_BENEFIT_VIEW_PERMISSION)))
    private val user = User(id = 42, roles = mutableSetOf(role))
    private val principal = JwtPrincipal(42, null, role.name, listOf(OWN_BENEFIT_VIEW_PERMISSION))
    private val service = CitizenBenefitGrantService(grants, users, current, BenefitGrantValidator())
    private fun authorize() {
        `when`(current.principal()).thenReturn(principal)
        `when`(users.findByIdWithRoles(42)).thenReturn(user)
    }

    @Test fun `consulta por titular autenticado y distingue ausencia total de pagina fuera de rango`() {
        authorize()
        val first = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "assignedAt", "id"))
        `when`(grants.findAllByApplicationUserId(42, first)).thenReturn(PageImpl<BenefitGrant>(emptyList(), first, 0))
        val empty = service.list(0, 20)
        assertEquals(CitizenBenefitGrantService.EMPTY_MESSAGE, empty.message)
        assertEquals(0L, empty.totalElements)
        assertTrue(empty.content.isEmpty())
        val outside = first.withPage(4)
        `when`(grants.findAllByApplicationUserId(42, outside)).thenReturn(PageImpl<BenefitGrant>(emptyList(), outside, 5))
        val page = service.list(4, 20)
        assertEquals(4, page.page)
        assertEquals(5L, page.totalElements)
        assertNull(page.message)
        verify(grants).findAllByApplicationUserId(42, first)
        verify(grants).findAllByApplicationUserId(42, outside)
    }

    @Test fun `permiso retirado del rol activo bloquea lectura aunque siga en JWT`() {
        authorize()
        role.permissions.clear()
        assertFailsWith<ForbiddenException> { service.list(0, 20) }
        verifyNoInteractions(grants)
    }

    @Test fun `permiso nuevo requiere token renovado aunque el rol ya lo tenga`() {
        authorize()
        `when`(current.principal()).thenReturn(principal.copy(permissions = emptyList()))
        assertFailsWith<ForbiddenException> { service.list(0, 20) }
        verifyNoInteractions(grants)
    }

    @Test fun `usuario inactivo no puede leer sus otorgamientos`() {
        authorize()
        user.active = false
        assertFailsWith<UnauthorizedException> { service.list(0, 20) }
        verifyNoInteractions(grants)
    }
}
