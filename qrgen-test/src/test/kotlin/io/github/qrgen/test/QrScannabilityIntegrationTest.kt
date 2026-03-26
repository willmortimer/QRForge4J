package io.github.qrgen.test

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QrScannabilityIntegrationTest {
    private val verifier = QrVerifier()

    @Test
    fun `baseline qr decodes across all formats`() {
        val data = "QRForge4J baseline"
        val results = verifier.verifyAcrossFormats(data, QrStyledCases.scannabilityCases(data).first().config)

        results.forEach { (format, result) ->
            assertTrue(result.isSuccessful, "Expected $format verification to succeed but got ${result.errorMessage}")
            assertEquals(data, result.decodedContent, "Decoded content mismatch for $format")
        }
    }

    @Test
    fun `styled cases remain scannable across all formats`() {
        QrStyledCases.scannabilityCases().forEach { testCase ->
            verifier.verifyAcrossFormats(testCase.data, testCase.config).forEach { (format, result) ->
                assertTrue(
                    result.isSuccessful && result.contentMatches,
                    "Expected ${testCase.name} to decode for $format but got ${result.errorMessage}"
                )
            }
        }
    }
}
