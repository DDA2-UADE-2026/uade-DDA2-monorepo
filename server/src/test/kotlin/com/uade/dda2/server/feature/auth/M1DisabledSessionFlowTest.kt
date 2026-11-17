package com.uade.dda2.server.feature.auth

import com.uade.dda2.server.security.ACCESS_COOKIE
import com.uade.dda2.server.security.CSRF_COOKIE
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class M1DisabledSessionFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper

    @Test fun `ruta desactivada responde 503 sin iniciar sesion ni requerir esquema de cuentas`() {
        val csrf = mvc.perform(get("/auth/session/csrf")).andExpect(status().isOk).andReturn().response
        mvc.perform(post("/auth/session/m1/login").cookie(assertNotNull(csrf.getCookie(CSRF_COOKIE)))
            .header("X-XSRF-TOKEN", json.readTree(csrf.contentAsString)["token"].asString())
            .contentType(MediaType.APPLICATION_JSON).content("""{"email":"ciudadano@example.invalid","password":"example-password"}"""))
            .andExpect(status().isServiceUnavailable).andExpect(jsonPath("$.code").value("M1_AUTH_DISABLED"))
            .andExpect(cookie().doesNotExist(ACCESS_COOKIE))
    }
}
