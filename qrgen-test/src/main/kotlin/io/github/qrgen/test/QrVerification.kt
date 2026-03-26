package io.github.qrgen.test

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.github.qrgen.core.AdvancedOptions
import io.github.qrgen.core.AlignmentPatternOptions
import io.github.qrgen.core.AlignmentPatternShape
import io.github.qrgen.core.AnimationOptions
import io.github.qrgen.core.AnimationPreset
import io.github.qrgen.core.BackgroundPattern
import io.github.qrgen.core.ColorOptions
import io.github.qrgen.core.DefaultQrGenerator
import io.github.qrgen.core.DotType
import io.github.qrgen.core.LayoutOptions
import io.github.qrgen.core.LocatorCornerStyle
import io.github.qrgen.core.LocatorDotShape
import io.github.qrgen.core.LocatorFrameShape
import io.github.qrgen.core.LocatorLogoOptions
import io.github.qrgen.core.LocatorOptions
import io.github.qrgen.core.ModuleOptions
import io.github.qrgen.core.PatternType
import io.github.qrgen.core.QrResult
import io.github.qrgen.core.QrOptions
import io.github.qrgen.core.QrStyleConfig
import io.github.qrgen.dsl.QRCode
import io.github.qrgen.dsl.QrCodeBuilder
import io.github.qrgen.pdf.QrPdfRenderer
import io.github.qrgen.png.BatikPngRenderer
import io.github.qrgen.png.PngRenderConfig
import io.github.qrgen.svg.DefaultSvgRenderer
import io.nayuki.qrcodegen.QrCode
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.time.Duration
import kotlin.time.measureTime

enum class VerificationFormat { SVG, PNG, JPEG, PDF }

/** QR code verification result **/
data class QrVerificationResult(
    val isSuccessful: Boolean,
    val decodedContent: String? = null,
    val expectedContent: String? = null,
    val errorCorrection: ErrorCorrectionLevel? = null,
    val version: Int? = null,
    val scanDuration: Duration? = null,
    val errorMessage: String? = null,
    val format: VerificationFormat = VerificationFormat.SVG
) {
    val contentMatches: Boolean get() = decodedContent == expectedContent
}

/** Performance metrics for QR generation and scanning **/
data class QrPerformanceMetrics(
    val generationTime: Duration,
    val svgSize: Int,
    val renderingTime: Duration? = null,
    val scanTime: Duration? = null,
    val isScannableByZXing: Boolean = false
)

data class StyledVerificationCase(
    val name: String,
    val data: String,
    val config: QrStyleConfig
)

/** ZXing-based QR code verification utilities **/
class QrVerifier(
    private val generator: DefaultQrGenerator = DefaultQrGenerator(),
    private val svgRenderer: DefaultSvgRenderer = DefaultSvgRenderer(),
    private val pngRenderer: BatikPngRenderer = BatikPngRenderer(),
    private val pdfRenderer: QrPdfRenderer = QrPdfRenderer()
) {
    private val decodeHints = mapOf(
        DecodeHintType.TRY_HARDER to true
    )
    private val reader = MultiFormatReader()

    fun verify(data: String, config: QrStyleConfig, format: VerificationFormat = VerificationFormat.SVG): QrVerificationResult {
        return try {
            val qrResult = generator.generateFromText(data, config)
            val image = renderToBufferedImage(qrResult, format)
            lateinit var result: Result
            val scanDuration = measureTime {
                result = scanQrCode(image)
            }
            QrVerificationResult(
                isSuccessful = true,
                decodedContent = result.text,
                expectedContent = data,
                errorCorrection = result.resultMetadata[ResultMetadataType.ERROR_CORRECTION_LEVEL] as? ErrorCorrectionLevel,
                version = qrResult.qrCode.version,
                scanDuration = scanDuration,
                format = format
            )
        } catch (e: Exception) {
            QrVerificationResult(
                isSuccessful = false,
                expectedContent = data,
                errorMessage = e.message ?: e::class.simpleName,
                format = format
            )
        }
    }

    fun verifyAcrossFormats(data: String, config: QrStyleConfig): Map<VerificationFormat, QrVerificationResult> {
        return VerificationFormat.entries.associateWith { verify(data, config, it) }
    }

    fun verify(data: String, builderBlock: QrCodeBuilder.() -> Unit): QrVerificationResult {
        val builder = QRCode.custom()
        builder.builderBlock()
        return verify(data, builder.build(data).config)
    }

    fun measurePerformance(data: String, config: QrStyleConfig): QrPerformanceMetrics {
        lateinit var qrResult: QrResult
        lateinit var svg: String

        val generationTime = measureTime {
            qrResult = generator.generateFromText(data, config)
        }
        val renderingTime = measureTime {
            svg = svgRenderer.render(qrResult)
        }
        val scanTime = measureTime {
            scanQrCode(renderSvgToBufferedImage(svg))
        }

        return QrPerformanceMetrics(
            generationTime = generationTime,
            svgSize = svg.length,
            renderingTime = renderingTime,
            scanTime = scanTime,
            isScannableByZXing = runCatching { scanQrCode(renderSvgToBufferedImage(svg)) }.isSuccess
        )
    }

    fun verifyBatch(testCases: List<Pair<String, QrStyleConfig>>, format: VerificationFormat = VerificationFormat.SVG): List<QrVerificationResult> {
        return testCases.map { (data, config) -> verify(data, config, format) }
    }

    fun testStylingCompatibility(data: String): Map<String, Map<VerificationFormat, QrVerificationResult>> {
        return QrStyledCases.defaultCases(data).associate { it.name to verifyAcrossFormats(it.data, it.config) }
    }

    fun renderToBufferedImage(qrResult: QrResult, format: VerificationFormat): BufferedImage {
        return when (format) {
            VerificationFormat.SVG -> renderSvgToBufferedImage(svgRenderer.render(qrResult))
            VerificationFormat.PNG -> decodeImage(pngRenderer.render(qrResult))
            VerificationFormat.JPEG -> decodeImage(pngRenderer.renderJpeg(qrResult))
            VerificationFormat.PDF -> renderPdfFirstPage(pdfRenderer.render(qrResult))
        }
    }

    fun renderSvgToBufferedImage(svg: String): BufferedImage {
        val png = PNGTranscoder()
        png.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 2048f)
        png.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 2048f)
        val output = java.io.ByteArrayOutputStream()
        png.transcode(
            org.apache.batik.transcoder.TranscoderInput(svg.reader()),
            org.apache.batik.transcoder.TranscoderOutput(output)
        )
        return decodeImage(output.toByteArray())
    }

    private fun renderPdfFirstPage(pdfBytes: ByteArray): BufferedImage {
        Loader.loadPDF(pdfBytes).use { document ->
            return PDFRenderer(document).renderImageWithDPI(0, 300f)
        }
    }

    private fun decodeImage(bytes: ByteArray): BufferedImage {
        return ImageIO.read(ByteArrayInputStream(bytes))
            ?: error("Unable to decode rendered image")
    }

    private fun scanQrCode(image: BufferedImage): Result {
        val candidates = listOf(
            compositeOnWhite(image),
            increaseContrast(compositeOnWhite(image))
        )

        val failures = mutableListOf<Throwable>()
        for (candidate in candidates) {
            val source = BufferedImageLuminanceSource(candidate)
            val bitmaps = listOf(
                BinaryBitmap(HybridBinarizer(source)),
                BinaryBitmap(GlobalHistogramBinarizer(source))
            )
            for (bitmap in bitmaps) {
                try {
                    reader.reset()
                    return reader.decode(bitmap, decodeHints)
                } catch (t: Throwable) {
                    failures += t
                }
            }
        }

        throw failures.lastOrNull() ?: NotFoundException.getNotFoundInstance()
    }

    private fun compositeOnWhite(image: BufferedImage): BufferedImage {
        val composited = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val graphics = composited.createGraphics()
        graphics.color = java.awt.Color.WHITE
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()
        return composited
    }

    private fun increaseContrast(image: BufferedImage): BufferedImage {
        val contrasted = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        RescaleOp(1.15f, -8f, null).filter(image, contrasted)
        return contrasted
    }
}

/** Shared styled cases used by scannability, golden, and benchmark tests. */
object QrStyledCases {
    private const val INLINE_LOGO = "data:image/svg+xml;base64," +
        "PHN2ZyB4bWxucz0naHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmcnIHZpZXdCb3g9JzAgMCAyNCAyNCc+" +
        "PHJlY3Qgd2lkdGg9JzI0JyBoZWlnaHQ9JzI0JyByeD0nNicgZmlsbD0nIzAwMDAwMCcvPjxwYXRoIGQ9" +
        "J002IDEyaDEyJyBzdHJva2U9JyNmZmZmZmYnIHN0cm9rZS13aWR0aD0nMycgc3Ryb2tlLWxpbmVjYXA9" +
        "J3JvdW5kJy8+PHBhdGggZD0nTTEyIDZ2MTInIHN0cm9rZT0nI2ZmZmZmZicgc3Ryb2tlLXdpZHRoPScz" +
        "JyBzdHJva2UtbGluZWNhcD0ncm91bmQnLz48L3N2Zz4="

    private fun safeBaseConfig(): QrStyleConfig = QrStyleConfig(
        qrOptions = QrOptions(ecc = QrCode.Ecc.HIGH),
        layout = LayoutOptions(width = 768, height = 768, margin = 28),
        modules = ModuleOptions(type = DotType.SQUARE),
        colors = ColorOptions(foreground = "#000000", background = "#ffffff")
    )

    fun scannabilityCases(data: String = "QRForge4J integration test"): List<StyledVerificationCase> {
        val safeBase = safeBaseConfig()

        return listOf(
            StyledVerificationCase("default", data, safeBase),
            StyledVerificationCase(
                "round_size_safe",
                data,
                safeBase.copy(modules = ModuleOptions(type = DotType.SQUARE, roundSize = true, sizeScale = 0.985))
            ),
            StyledVerificationCase(
                "rounded_background",
                data,
                safeBase.copy(layout = safeBase.layout.copy(backgroundCornerRadius = 28.0))
            ),
            StyledVerificationCase(
                "gradient_pattern",
                data,
                safeBase.copy(
                    advanced = AdvancedOptions(
                        backgroundPattern = BackgroundPattern(enabled = true, type = PatternType.DOTS, opacity = 0.006, size = 18.0)
                    )
                )
            ),
            StyledVerificationCase(
                "animation_presence",
                data,
                safeBase.copy(animation = AnimationOptions(enabled = true, preset = AnimationPreset.FADE))
            )
        )
    }

    fun defaultCases(data: String = "QRForge4J integration test"): List<StyledVerificationCase> {
        val safeBase = QrStyleConfig(
            qrOptions = QrOptions(ecc = QrCode.Ecc.HIGH),
            layout = LayoutOptions(width = 768, height = 768, margin = 28),
            modules = ModuleOptions(type = DotType.SQUARE),
            colors = ColorOptions(foreground = "#000000", background = "#ffffff")
        )

        return listOf(
            StyledVerificationCase("default", data, safeBase),
            StyledVerificationCase(
                "rounded_smoothing",
                data,
                safeBase.copy(
                    modules = ModuleOptions(type = DotType.ROUNDED, roundSize = true, sizeScale = 0.985, radiusFactor = 0.18)
                )
            ),
            StyledVerificationCase(
                "corner_logos",
                data,
                safeBase.copy(
                    locators = LocatorOptions(
                        enabled = true,
                        defaultStyle = LocatorCornerStyle(
                            outerShape = LocatorFrameShape.SQUARE,
                            innerShape = LocatorDotShape.SQUARE,
                            logo = LocatorLogoOptions(INLINE_LOGO, 0.14)
                        )
                    )
                )
            ),
            StyledVerificationCase(
                "mixed_corners_alignment",
                data,
                safeBase.copy(
                    locators = LocatorOptions(
                        enabled = true,
                        defaultStyle = LocatorCornerStyle(outerShape = LocatorFrameShape.SQUARE, innerShape = LocatorDotShape.SQUARE),
                        topRight = LocatorCornerStyle(outerShape = LocatorFrameShape.ROUNDED, innerShape = LocatorDotShape.CIRCLE),
                        bottomLeft = LocatorCornerStyle(outerShape = LocatorFrameShape.SQUARE, innerShape = LocatorDotShape.DIAMOND)
                    ),
                    alignmentPatterns = AlignmentPatternOptions(enabled = true, shape = AlignmentPatternShape.DIAMOND, sizeRatio = 0.82)
                )
            ),
            StyledVerificationCase(
                "rounded_background",
                data,
                safeBase.copy(layout = safeBase.layout.copy(backgroundCornerRadius = 28.0))
            ),
            StyledVerificationCase(
                "gradient_pattern",
                data,
                safeBase.copy(
                    advanced = AdvancedOptions(
                        backgroundPattern = BackgroundPattern(enabled = true, type = PatternType.DOTS, opacity = 0.006, size = 18.0)
                    )
                )
            ),
            StyledVerificationCase(
                "animation_presence",
                data,
                safeBase.copy(animation = AnimationOptions(enabled = true, preset = AnimationPreset.FADE))
            )
        )
    }
}

/** Utility functions for common test scenarios **/
object QrTestUtils {
    fun testAllModuleStyles(data: String = "Test QR Code"): Map<String, QrVerificationResult> {
        val verifier = QrVerifier()
        return DotType.entries.associate { dotType ->
            dotType.name to verifier.verify(data, QrStyleConfig(modules = ModuleOptions(type = dotType)))
        }
    }

    fun benchmarkPerformance(iterations: Int = 100): List<QrPerformanceMetrics> {
        val verifier = QrVerifier()
        return List(iterations) { i ->
            verifier.measurePerformance("Performance test iteration $i", QrStyleConfig())
        }
    }
}
