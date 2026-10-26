package com.uade.dda2.server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TimeConfiguration {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
