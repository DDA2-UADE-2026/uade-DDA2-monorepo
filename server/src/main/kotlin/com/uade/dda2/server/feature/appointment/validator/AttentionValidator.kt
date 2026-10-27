package com.uade.dda2.server.feature.appointment.validator

import com.uade.dda2.server.feature.appointment.entity.AttentionResult
import com.uade.dda2.server.feature.appointment.error.AttentionErrors
import org.springframework.stereotype.Component
import java.time.LocalDate

data class ValidatedAttention(val attendedOn: LocalDate?, val description: String?)

@Component
class AttentionValidator {
    fun validate(result: AttentionResult, attendedOn: LocalDate?, description: String?, today: LocalDate): ValidatedAttention {
        if (result == AttentionResult.AUSENTE) {
            if (attendedOn != null || description != null) {
                throw AttentionErrors.invalidData("Una ausencia no admite fecha ni descripcion de atencion.")
            }
            return ValidatedAttention(null, null)
        }
        if (attendedOn == null || attendedOn.isAfter(today)) {
            throw AttentionErrors.invalidData("La fecha de atencion es obligatoria y no puede ser futura.")
        }
        val cleaned = description?.trim()
        if (cleaned.isNullOrEmpty() || cleaned.length > 500) {
            throw AttentionErrors.invalidData("Describa el servicio prestado en 1 a 500 caracteres.")
        }
        return ValidatedAttention(attendedOn, cleaned)
    }
}
