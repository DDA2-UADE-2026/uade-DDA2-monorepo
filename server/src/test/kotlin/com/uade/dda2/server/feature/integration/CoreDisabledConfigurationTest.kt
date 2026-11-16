package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.feature.integration.client.CoreTokenClient
import com.uade.dda2.server.feature.integration.service.CoreOutboxPublisher
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("docs")
class CoreDisabledConfigurationTest {
    @Autowired lateinit var context: ApplicationContext

    @Test fun `docs arranca sin credenciales ni objetos que puedan conectar a Core`() {
        assertTrue(context.getBeansOfType(ConnectionFactory::class.java).isEmpty())
        assertTrue(context.getBeansOfType(CoreTokenClient::class.java).isEmpty())
        assertTrue(context.getBeansOfType(CoreOutboxPublisher::class.java).isEmpty())
    }
}
