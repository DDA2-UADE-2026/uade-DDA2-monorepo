package com.uade.dda2.server.feature.log.entity

enum class LogEntityType(val tableName: String) {
    PERMISSION("permissions"),
    ROLE("roles"),
    USER("users"),
    ENROLLMENT_PERIOD("enrollment_period"),
    APPLICATION("application"),
    APPLICATION_DOCUMENT("application_document"),
    ACTIVITY("activity"),
    ACTIVITY_ENROLLMENT("activity_enrollment"),
    MUNICIPAL_CENTER("municipal_center"),
    MUNICIPAL_SERVICE("municipal_service"),
    CENTER_SERVICE("center_service"),
    PROGRAM("program"),
    PROGRAM_EDITION("program_edition"),
    PROGRAM_BENEFIT("program_benefit"),
    PROGRAM_REQUIREMENT("program_requirement"),
    PROGRAM_DOCUMENT_REQUIREMENT("program_document_requirement"),
    PROGRAM_IMAGE("program_image"),
    PROGRAM_INCOMPATIBILITY("program_incompatibility"),
    ;

    companion object {
        private val valuesByTableName = entries.associateBy(LogEntityType::tableName)

        fun fromTableName(tableName: String): LogEntityType =
            valuesByTableName[tableName]
                ?: throw IllegalArgumentException("Tipo de entidad de log desconocido: $tableName")
    }
}
