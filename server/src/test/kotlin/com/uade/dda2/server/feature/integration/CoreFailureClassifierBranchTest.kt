package com.uade.dda2.server.feature.integration

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AuthenticationFailureException
import com.rabbitmq.client.ShutdownSignalException
import com.uade.dda2.server.feature.integration.util.CoreFailureClassifier
import org.springframework.amqp.AmqpAuthenticationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.net.*
import java.net.http.HttpTimeoutException
import javax.net.ssl.SSLException
import kotlin.test.assertEquals

class CoreFailureClassifierBranchTest {
    @TestFactory
    fun `transport failures map to stable codes without exposing messages`() = listOf(
        AuthenticationFailureException("secret") to "CORE_AMQP_AUTHENTICATION_FAILED",
        AmqpAuthenticationException(IllegalStateException("secret")) to "CORE_AMQP_AUTHENTICATION_FAILED",
        SSLException("secret") to "CORE_TLS_FAILURE",
        UnknownHostException("secret") to "CORE_DNS_FAILURE",
        SocketTimeoutException("secret") to "CORE_CONNECTION_TIMEOUT",
        HttpTimeoutException("secret") to "CORE_CONNECTION_TIMEOUT",
        ConnectException("secret") to "CORE_CONNECTION_FAILED",
        IllegalStateException("secret") to "FALLBACK",
    ).flatMap { (exception, expected) -> listOf(false, true).map { wrapped -> dynamicTest("${exception.javaClass.simpleName} wrapped=$wrapped") {
        val failure = if (wrapped) RuntimeException("do not disclose", exception) else exception
        assertEquals(expected, CoreFailureClassifier.code(failure, "FALLBACK"))
    } } }

    @TestFactory
    fun `channel and connection close codes are classified`() = listOf(403, 404, 406, 500).flatMap { status ->
        listOf(false, true).map { connection -> dynamicTest("reply=$status connection=$connection") {
            val reason = if (connection) AMQP.Connection.Close.Builder().replyCode(status).replyText("secret").classId(0).methodId(0).build()
                else AMQP.Channel.Close.Builder().replyCode(status).replyText("secret").classId(0).methodId(0).build()
            val expected = when (status) {
                403 -> "CORE_AMQP_ACCESS_DENIED"
                404 -> "CORE_AMQP_RESOURCE_NOT_FOUND"
                406 -> "CORE_AMQP_PRECONDITION_FAILED"
                else -> "FALLBACK"
            }
            assertEquals(expected, CoreFailureClassifier.code(ShutdownSignalException(connection, false, reason, null), "FALLBACK"))
        } }
    }

    @Test
    fun `unknown shutdown reason falls back and cause scanning is bounded`() {
        assertEquals("FALLBACK", CoreFailureClassifier.code(ShutdownSignalException(false, false, null, null), "FALLBACK"))
        var failure: Throwable = UnknownHostException("secret")
        repeat(16) { failure = RuntimeException(failure) }
        assertEquals("FALLBACK", CoreFailureClassifier.code(failure, "FALLBACK"))
        assertEquals("CORE_TLS_FAILURE", CoreFailureClassifier.code(SSLException("secret", UnknownHostException()), "FALLBACK"))
    }
}
