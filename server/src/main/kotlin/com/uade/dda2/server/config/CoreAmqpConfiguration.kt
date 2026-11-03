package com.uade.dda2.server.config

import com.rabbitmq.client.ConnectionFactory as RabbitConnectionFactory
import com.uade.dda2.server.feature.integration.client.*
import com.uade.dda2.server.feature.integration.messaging.CoreAmqpPublisher
import com.uade.dda2.server.feature.integration.messaging.CoreMessageListener
import com.uade.dda2.server.feature.integration.service.*
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.boot.amqp.autoconfigure.RabbitProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.net.http.HttpClient
import java.time.Clock
import javax.net.ssl.SSLContext

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.core", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(RabbitProperties::class)
class CoreAmqpConfiguration {
    @Bean
    fun coreConnectionFactory(rabbit: RabbitProperties, core: CoreProperties): CachingConnectionFactory {
        require(rabbit.username.isNotBlank() && rabbit.password.isNotBlank()) { "Faltan credenciales AMQPS de Core." }
        require(rabbit.host.isNotBlank() && !rabbit.virtualHost.isNullOrBlank()) { "Falta host o vhost de Core." }
        val tls = rabbit.ssl.determineEnabled()
        require(tls || rabbit.host in CoreProperties.LOOPBACK_HOSTS) { "Core remoto requiere AMQPS." }
        require(rabbit.ssl.isValidateServerCertificate && rabbit.ssl.isVerifyHostname) { "No se puede desactivar la validación TLS de Core." }
        val native = RabbitConnectionFactory().apply {
            host = rabbit.host
            port = rabbit.determinePort()
            username = rabbit.username
            password = rabbit.password
            virtualHost = rabbit.virtualHost
            connectionTimeout = core.connectTimeout.toMillis().toInt()
            handshakeTimeout = core.readTimeout.toMillis().toInt()
            channelRpcTimeout = core.readTimeout.toMillis().toInt()
            requestedHeartbeat = 30
            isAutomaticRecoveryEnabled = false // La recuperación la maneja Spring AMQP.
            if (tls) { useSslProtocol(SSLContext.getDefault()); enableHostnameVerification() }
        }
        return CachingConnectionFactory(native).apply {
            setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED)
            isPublisherReturns = true
        }
    }

    @Bean
    fun coreRabbitTemplate(factory: CachingConnectionFactory): RabbitTemplate = RabbitTemplate(factory).apply {
        setMandatory(true)
    }

    @Bean
    fun coreHttpTransport(core: CoreProperties, json: JsonMapper): CoreHttpTransport {
        val client = HttpClient.newBuilder().connectTimeout(core.connectTimeout).build()
        val requestFactory = JdkClientHttpRequestFactory(client).apply { setReadTimeout(core.readTimeout) }
        return CoreHttpTransport(RestClient.builder().requestFactory(requestFactory).build(), json, core.apiBaseUrl)
    }

    @Bean fun coreTokenClient(transport: CoreHttpTransport, json: JsonMapper, core: CoreProperties, clock: Clock) =
        CoreTokenClient(transport, json, core, clock)
    @Bean fun coreApiClient(transport: CoreHttpTransport, tokens: CoreTokenClient) = CoreApiClient(transport, tokens)
    @Bean fun coreRegistryClient(api: CoreApiClient, json: JsonMapper, core: CoreProperties) = CoreRegistryClient(api, json, core)
    @Bean fun coreEventClient(api: CoreApiClient, json: JsonMapper) = CoreEventClient(api, json)
    @Bean fun coreDashboardClient(api: CoreApiClient, json: JsonMapper, core: CoreProperties) = CoreDashboardClient(api, json, core)
    @Bean fun coreBrokerClient(factory: CachingConnectionFactory, core: CoreProperties) = CoreBrokerClient(factory, core)
    @Bean fun coreAmqpPublisher(template: RabbitTemplate, core: CoreProperties, rabbit: RabbitProperties) =
        CoreAmqpPublisher(template, core, rabbit.username)

    @Bean
    @ConditionalOnProperty(prefix = "app.core", name = ["check-on-startup"], havingValue = "true")
    fun coreConnectionCheckRunner(broker: CoreBrokerClient, core: CoreProperties, rabbit: RabbitProperties) =
        CoreConnectionCheckRunner(broker, core, rabbit)

    @Bean
    @ConditionalOnProperty(prefix = "app.core.probe", name = ["enabled"], havingValue = "true")
    fun coreProbeHandler(core: CoreProperties) = CoreProbeHandler(core)

    @Bean
    @ConditionalOnProperty(prefix = "app.core.publisher", name = ["enabled"], havingValue = "true")
    fun coreOutboxPublisher(store: CoreOutboxStore, sender: CoreAmqpPublisher, core: CoreProperties) = CoreOutboxPublisher(store, sender, core)

    @Bean
    fun coreListenerContainer(factory: CachingConnectionFactory, inbox: CoreInboxService, core: CoreProperties): SimpleMessageListenerContainer =
        SimpleMessageListenerContainer(factory).apply {
            setQueueNames(core.queue)
            setAutoDeclare(false)
            setAutoStartup(core.consumer.enabled)
            setMissingQueuesFatal(false)
            setDefaultRequeueRejected(false)
            setPrefetchCount(core.consumer.prefetch)
            setConcurrentConsumers(1)
            acknowledgeMode = AcknowledgeMode.MANUAL
            setMessageListener(CoreMessageListener(inbox))
        }
}
