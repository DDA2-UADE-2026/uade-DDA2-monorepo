package com.uade.dda2.server.feature.center.entity

import org.junit.jupiter.api.Test
import java.text.Normalizer
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MunicipalNameNormalizerTest {
    @Test
    fun `normaliza a NFC recorta y colapsa whitespace`() {
        val decomposed = Normalizer.normalize("ATENCIÓN", Normalizer.Form.NFD)

        val normalized = MunicipalNameNormalizer.normalize("  $decomposed\t  Familiar\n ")

        assertEquals("atención familiar", normalized)
        assertEquals(Normalizer.Form.NFC, detectedForm(normalized))
    }

    @Test
    fun `conserva acentos`() {
        val withoutAccent = MunicipalNameNormalizer.normalize("Atencion")
        val withAccent = MunicipalNameNormalizer.normalize("Atención")

        assertNotEquals(withoutAccent, withAccent)
        assertEquals("atención", withAccent)
    }

    @Test
    fun `usa locale root aunque cambie el locale por defecto`() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("instituto", MunicipalNameNormalizer.normalize("INSTITUTO"))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    private fun detectedForm(value: String): Normalizer.Form =
        if (Normalizer.isNormalized(value, Normalizer.Form.NFC)) {
            Normalizer.Form.NFC
        } else {
            Normalizer.Form.NFD
        }
}
