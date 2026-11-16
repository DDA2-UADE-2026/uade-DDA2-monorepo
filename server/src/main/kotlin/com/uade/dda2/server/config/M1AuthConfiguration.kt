package com.uade.dda2.server.config

import com.uade.dda2.server.feature.integration.client.M1AuthClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.net.http.HttpClient

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.m1.auth", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(M1AuthProperties::class)
class M1AuthConfiguration {
    @Bean
    fun m1AuthClient(properties: M1AuthProperties, json: JsonMapper): M1AuthClient {
        val http = HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()
        val factory = JdkClientHttpRequestFactory(http).apply { setReadTimeout(properties.readTimeout) }
        return M1AuthClient(RestClient.builder().requestFactory(factory).build(), json, properties)
    }
}
