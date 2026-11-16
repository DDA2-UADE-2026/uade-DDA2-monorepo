package com.uade.dda2.server.feature.integration.util

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AuthenticationFailureException
import com.rabbitmq.client.ShutdownSignalException
import org.springframework.amqp.AmqpAuthenticationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.http.HttpTimeoutException
import javax.net.ssl.SSLException

object CoreFailureClassifier {
    // Solo inspecciona tipos y códigos de protocolo; nunca copia mensajes, URLs o credenciales.
    fun code(exception: Throwable, fallback: String): String {
        val causes = generateSequence(exception) { it.cause }.take(16).toList()
        if (causes.any { it is AuthenticationFailureException || it is AmqpAuthenticationException }) return "CORE_AMQP_AUTHENTICATION_FAILED"
        if (causes.any { it is SSLException }) return "CORE_TLS_FAILURE"
        if (causes.any { it is UnknownHostException }) return "CORE_DNS_FAILURE"
        if (causes.any { it is SocketTimeoutException || it is HttpTimeoutException }) return "CORE_CONNECTION_TIMEOUT"
        if (causes.any { it is ConnectException }) return "CORE_CONNECTION_FAILED"
        for (cause in causes.filterIsInstance<ShutdownSignalException>()) {
            val status = when (val reason = cause.reason) {
                is AMQP.Channel.Close -> reason.replyCode
                is AMQP.Connection.Close -> reason.replyCode
                else -> null
            }
            when (status) {
                403 -> return "CORE_AMQP_ACCESS_DENIED"
                404 -> return "CORE_AMQP_RESOURCE_NOT_FOUND"
                406 -> return "CORE_AMQP_PRECONDITION_FAILED"
            }
        }
        return fallback
    }
}
