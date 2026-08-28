package com.uade.dda2.server.feature.log.entity

enum class LogEntityType(val tableName: String) {
    PERMISSION("permissions"),
    ROLE("roles"),
    USER("users"),
    ENROLLMENT_PERIOD("enrollment_period"),
    ;

    companion object {
        private val valuesByTableName = entries.associateBy(LogEntityType::tableName)

        fun fromTableName(tableName: String): LogEntityType =
            valuesByTableName[tableName]
                ?: throw IllegalArgumentException("Tipo de entidad de log desconocido: $tableName")
    }
}
