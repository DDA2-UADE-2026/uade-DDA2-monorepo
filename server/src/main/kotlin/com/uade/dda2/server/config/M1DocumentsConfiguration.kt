package com.uade.dda2.server.config

import com.uade.dda2.server.feature.integration.client.M1DocumentClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

@Configuration
@ConditionalOnProperty(prefix = "app.m1.documents", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(M1DocumentsProperties::class)
class M1DocumentsConfiguration {
    @Bean
    fun m1DocumentClient(properties: M1DocumentsProperties): M1DocumentClient {
        val http = HttpClient.newBuilder().connectTimeout(properties.connectTimeout)
            .followRedirects(HttpClient.Redirect.NEVER).build()
        val factory = JdkClientHttpRequestFactory(http).apply { setReadTimeout(properties.readTimeout) }
        return M1DocumentClient(RestClient.builder().requestFactory(factory).build(), properties)
    }
}
