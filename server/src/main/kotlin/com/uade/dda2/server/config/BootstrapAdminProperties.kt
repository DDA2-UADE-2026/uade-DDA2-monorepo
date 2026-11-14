package com.uade.dda2.server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.bootstrap.admin")
// No data class: generated toString must not include the bootstrap password.
class BootstrapAdminProperties(
    val enabled: Boolean = true,
    val password: String = "",
)
