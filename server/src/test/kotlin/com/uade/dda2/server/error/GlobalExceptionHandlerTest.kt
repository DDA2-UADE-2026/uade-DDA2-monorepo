package com.uade.dda2.server.error

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `el exceso de tamano distingue la imagen de programa de los documentos`() {
        val image = handler.handleMaxUploadSize(MockHttpServletRequest("PUT", "/api/admin/programs/p-1/image"))
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, image.statusCode)
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, image.headers.contentType)
        assertEquals("PROGRAM_IMAGE_FILE_TOO_LARGE", image.body!!.code)
        assertEquals("La imagen no puede superar 10 MB.", image.body!!.message)
        assertEquals("/api/admin/programs/p-1/image", image.body!!.path)

        val document = handler.handleMaxUploadSize(MockHttpServletRequest("POST", "/api/applications/a-1/documents"))
        assertEquals("APPLICATION_DOCUMENT_FILE_TOO_LARGE", document.body!!.code)
        assertEquals("El archivo no puede superar 10 MB.", document.body!!.message)
    }

    @Test
    fun `las credenciales invalidas responden 401 sin detallar la causa`() {
        val response = handler.handleAuthentication(MockHttpServletRequest("POST", "/auth/login"))
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("AUTH_INVALID_CREDENTIALS", response.body!!.code)
        assertEquals(401, response.body!!.status)
        assertEquals("/auth/login", response.body!!.path)
    }
}
