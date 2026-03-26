package io.github.qrgen.test

import io.github.qrgen.core.DefaultQrGenerator
import io.github.qrgen.pdf.QrPdfRenderer
import io.github.qrgen.png.BatikPngRenderer
import io.github.qrgen.svg.DefaultSvgRenderer
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object QrGoldenSupport {
    private val generator = DefaultQrGenerator()
    private val svgRenderer = DefaultSvgRenderer()
    private val pngRenderer = BatikPngRenderer()
    private val pdfRenderer = QrPdfRenderer()
    private val verifier = QrVerifier()

    fun normalizedSvg(case: StyledVerificationCase): String {
        val qrResult = generator.generateFromText(case.data, case.config)
        val svg = svgRenderer.render(qrResult)
        return normalizeSvg(svg)
    }

    fun rasterSmokeImage(case: StyledVerificationCase, format: VerificationFormat): BufferedImage {
        val qrResult = generator.generateFromText(case.data, case.config)
        return when (format) {
            VerificationFormat.SVG -> verifier.renderSvgToBufferedImage(svgRenderer.render(qrResult))
            VerificationFormat.PNG -> ImageIO.read(java.io.ByteArrayInputStream(pngRenderer.render(qrResult)))
            VerificationFormat.JPEG -> ImageIO.read(java.io.ByteArrayInputStream(pngRenderer.renderJpeg(qrResult)))
            VerificationFormat.PDF -> verifier.renderToBufferedImage(qrResult, VerificationFormat.PDF)
        } ?: error("Unable to decode raster smoke image for $format")
    }

    fun writeGoldens(outputDir: File) {
        val svgDir = File(outputDir, "svg").apply { mkdirs() }
        val rasterDir = File(outputDir, "raster").apply { mkdirs() }
        QrStyledCases.defaultCases().forEach { case ->
            File(svgDir, "${case.name}.svg").writeText(normalizedSvg(case))
        }
        listOf(
            QrStyledCases.defaultCases().first { it.name == "default" } to VerificationFormat.PNG,
            QrStyledCases.defaultCases().first { it.name == "corner_logos" } to VerificationFormat.JPEG,
            QrStyledCases.defaultCases().first { it.name == "mixed_corners_alignment" } to VerificationFormat.PDF
        ).forEach { (case, format) ->
            ImageIO.write(rasterSmokeImage(case, format), "png", File(rasterDir, "${case.name}-${format.name.lowercase()}.png"))
        }
    }

    fun normalizeSvg(svg: String): String {
        return svg
            .replace(Regex(">\\s+<"), "><")
            .replace(Regex("\\s+"), " ")
            .replace("\" />", "\"/>")
            .trim()
    }

    fun imageDiffRatio(expected: BufferedImage, actual: BufferedImage): Double {
        require(expected.width == actual.width && expected.height == actual.height) {
            "Image dimensions differ: expected ${expected.width}x${expected.height}, actual ${actual.width}x${actual.height}"
        }
        var different = 0L
        val total = expected.width.toLong() * expected.height.toLong()
        for (y in 0 until expected.height) {
            for (x in 0 until expected.width) {
                if (expected.getRGB(x, y) != actual.getRGB(x, y)) different++
            }
        }
        return different.toDouble() / total.toDouble()
    }
}
