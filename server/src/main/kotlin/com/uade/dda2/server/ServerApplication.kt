package com.uade.dda2.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
	exclude = [
		UserDetailsServiceAutoConfiguration::class,
	],
)
@ConfigurationPropertiesScan
@EnableMethodSecurity
@EnableScheduling
class ServerApplication

fun main(args: Array<String>) {
	runApplication<ServerApplication>(*args)
}
