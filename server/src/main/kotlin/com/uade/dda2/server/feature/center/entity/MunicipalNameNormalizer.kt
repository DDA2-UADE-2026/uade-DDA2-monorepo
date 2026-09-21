package com.uade.dda2.server.feature.center.entity

import java.text.Normalizer
import java.util.Locale

object MunicipalNameNormalizer {
    private val whitespace = Regex("\\s+")

    fun normalize(value: String): String =
        Normalizer
            .normalize(value, Normalizer.Form.NFC)
            .trim()
            .replace(whitespace, " ")
            .lowercase(Locale.ROOT)
}
