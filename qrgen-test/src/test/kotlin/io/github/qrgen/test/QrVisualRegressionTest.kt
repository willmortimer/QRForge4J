package io.github.qrgen.test

import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class QrVisualRegressionTest {
    @Test
    fun `normalized svg matches checked in goldens`() {
        val cases = listOf("default", "rounded_smoothing", "corner_logos", "mixed_corners_alignment", "rounded_background", "animation_presence")
        cases.forEach { name ->
            val testCase = QrStyledCases.defaultCases().first { it.name == name }
            val expected = resourceText("goldens/svg/$name.svg")
            val actual = QrGoldenSupport.normalizedSvg(testCase)
            assertTrue(expected == actual, "SVG golden mismatch for $name")
        }
    }

    @Test
    fun `raster smoke outputs stay within threshold`() {
        val cases = listOf(
            Triple("default", VerificationFormat.PNG, "default-png.png"),
            Triple("corner_logos", VerificationFormat.JPEG, "corner_logos-jpeg.png"),
            Triple("mixed_corners_alignment", VerificationFormat.PDF, "mixed_corners_alignment-pdf.png")
        )
        cases.forEach { (name, format, resourceName) ->
            val expected = ImageIO.read(checkNotNull(javaClass.classLoader.getResourceAsStream("goldens/raster/$resourceName")))
            val actual = QrGoldenSupport.rasterSmokeImage(QrStyledCases.defaultCases().first { it.name == name }, format)
            val diff = QrGoldenSupport.imageDiffRatio(expected, actual)
            assertTrue(diff <= 0.0, "Raster golden mismatch for $name / $format: diff=$diff")
        }
    }

    private fun resourceText(path: String): String {
        return checkNotNull(javaClass.classLoader.getResourceAsStream(path)).bufferedReader().use { it.readText() }
    }
}
