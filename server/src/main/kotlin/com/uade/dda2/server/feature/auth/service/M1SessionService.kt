package com.uade.dda2.server.feature.auth.service

import com.uade.dda2.server.feature.auth.dto.request.M1SessionLoginRequest
import com.uade.dda2.server.feature.auth.dto.response.LoginResponse
import com.uade.dda2.server.feature.integration.client.M1AuthClient
import com.uade.dda2.server.feature.integration.dto.M1LoginRequest
import com.uade.dda2.server.feature.integration.error.M1AuthException
import com.uade.dda2.server.feature.integration.error.M1AuthFailure
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class M1SessionService(
    private val clients: ObjectProvider<M1AuthClient>,
    private val accounts: M1CitizenAccountService,
    private val auth: AuthService,
) {
    // No mantener conexiones o bloqueos de la base mientras responde M1.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun login(request: M1SessionLoginRequest): LoginResponse {
        val client = clients.ifAvailable ?: throw M1AuthException(M1AuthFailure.DISABLED)
        val email = request.email.trim()
        val identity = client.login(M1LoginRequest(email, request.password))
        val account = accounts.resolve(identity, email)
        return auth.startM1Session(account)
    }
}
