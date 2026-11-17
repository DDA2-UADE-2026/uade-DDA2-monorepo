package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.config.M1DocumentsProperties
import com.uade.dda2.server.feature.integration.error.M1DocumentException
import com.uade.dda2.server.feature.integration.error.M1DocumentFailure
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit

class M1DocumentFile(val bytes: ByteArray, val contentType: MediaType, val filename: String)

class M1DocumentClient(private val client: RestClient, properties: M1DocumentsProperties) {
    private val origin = properties.baseUrl.trimEnd('/')
    private val path = properties.filePathTemplate
    private val token = properties.serviceBearerToken
    private val maxBytes = properties.maxFileBytes
    private val timeoutNanos = TimeUnit.MILLISECONDS.toNanos(properties.readTimeout.toMillis())

    fun download(documentId: UUID): M1DocumentFile {
        val started = System.nanoTime()
        try {
            return client.get().uri(URI.create(origin + path.replace("{documentId}", documentId.toString())))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange { _, response ->
                    val status = response.statusCode.value()
                    if (status != 200) throw M1DocumentException(when (status) {
                        401, 403 -> M1DocumentFailure.ACCESS_DENIED
                        404 -> M1DocumentFailure.FILE_NOT_FOUND
                        429, 503 -> M1DocumentFailure.UNAVAILABLE
                        504 -> M1DocumentFailure.TIMEOUT
                        in 200..399 -> M1DocumentFailure.INVALID_RESPONSE
                        else -> M1DocumentFailure.UPSTREAM_ERROR
                    })
                    if (response.headers.getFirst(HttpHeaders.CONTENT_ENCODING)?.equals("identity", ignoreCase = true) == false)
                        fail(M1DocumentFailure.INVALID_RESPONSE)
                    val type = response.headers.contentType
                    // No convertir páginas de login o errores JSON con HTTP 200 en un archivo.
                    if (type != null && (type.type == "text" || type.subtype == "json" || type.subtype.endsWith("+json")))
                        fail(M1DocumentFailure.INVALID_RESPONSE)
                    val declaredSize = response.headers.contentLength
                    if (declaredSize > maxBytes) fail(M1DocumentFailure.TOO_LARGE)
                    // Spring normaliza gzip antes de exchange; el límite rige sobre los bytes descomprimidos.
                    // El cuerpo se consume y se cierra aquí; nunca escapa un stream HTTP.
                    val bytes = response.body.readNBytes(maxBytes + 1)
                    if (bytes.size > maxBytes) fail(M1DocumentFailure.TOO_LARGE)
                    if (bytes.isEmpty() || declaredSize >= 0 && declaredSize != bytes.size.toLong())
                        fail(M1DocumentFailure.INVALID_RESPONSE)
                    val (safeType, extension) = when (type?.let { "${it.type}/${it.subtype}" }) {
                        "application/pdf" -> MediaType.APPLICATION_PDF to "pdf"
                        "image/png" -> MediaType.IMAGE_PNG to "png"
                        "image/jpeg" -> MediaType.IMAGE_JPEG to "jpg"
                        else -> MediaType.APPLICATION_OCTET_STREAM to "bin"
                    }
                    M1DocumentFile(bytes, safeType, "documento-$documentId.$extension")
                } ?: fail(M1DocumentFailure.INVALID_RESPONSE)
        } catch (exception: M1DocumentException) {
            throw exception
        } catch (exception: ResourceAccessException) {
            val timedOut = generateSequence<Throwable>(exception) { it.cause }.take(16)
                .any { it is HttpTimeoutException || it is SocketTimeoutException } || System.nanoTime() - started >= timeoutNanos
            fail(if (timedOut) M1DocumentFailure.TIMEOUT else M1DocumentFailure.UNAVAILABLE)
        } catch (_: Exception) {
            // No conservar cuerpos, encabezados ni mensajes que puedan contener credenciales.
            fail(M1DocumentFailure.INVALID_RESPONSE)
        }
    }

    private fun fail(failure: M1DocumentFailure): Nothing = throw M1DocumentException(failure)
}
