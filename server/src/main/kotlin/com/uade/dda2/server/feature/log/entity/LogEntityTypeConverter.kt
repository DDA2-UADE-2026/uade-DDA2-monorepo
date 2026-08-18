package com.uade.dda2.server.feature.log.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class LogEntityTypeConverter : AttributeConverter<LogEntityType, String> {
    override fun convertToDatabaseColumn(attribute: LogEntityType?): String? =
        attribute?.tableName

    override fun convertToEntityAttribute(dbData: String?): LogEntityType? =
        dbData?.let(LogEntityType::fromTableName)
}
