package io.github.qrgen.test

import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.github.qrgen.core.*
import io.github.qrgen.dsl.*
import io.github.qrgen.svg.DefaultSvgRenderer
import org.w3c.dom.Document
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration
import kotlin.time.measureTime

/** QR code verification result **/
data class QrVerificationResult(
    val isSuccessful: Boolean,
    val decodedContent: String? = null,
    val expectedContent: String? = null,
    val errorCorrection: ErrorCorrectionLevel? = null,
    val version: Int? = null,
    val scanDuration: Duration? = null,
    val errorMessage: String? = null
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

/** ZXing-based QR code verification utilities **/
class QrVerifier {
    private val reader = QRCodeReader()
    
    /**
     * Verify that a QR code configuration produces a scannable result
     */
    fun verify(data: String, config: QrStyleConfig): QrVerificationResult {
        return try {
            // Generate QR code
            val generator = DefaultQrGenerator()
            val qrResult = generator.generateFromText(data, config)
            
            // Render to SVG
            val renderer = DefaultSvgRenderer()
            val svg = renderer.render(qrResult)
            
            // Convert SVG to BufferedImage for ZXing
            val image = svgToBufferedImage(svg, config.layout.width, config.layout.height)
            
            // Scan with ZXing
            val scanDuration = measureTime {
                val result = scanQrCode(image)
                return QrVerificationResult(
                    isSuccessful = true,
                    decodedContent = result.text,
                    expectedContent = data,
                    errorCorrection = result.resultMetadata?.get(ResultMetadataType.ERROR_CORRECTION_LEVEL) as? ErrorCorrectionLevel
                )
            }
            
            QrVerificationResult(isSuccessful = false, errorMessage = "Scan completed but no result returned")
        } catch (e: Exception) {
            QrVerificationResult(
                isSuccessful = false,
                expectedContent = data,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Verify using the DSL builder
     */
    fun verify(data: String, builderBlock: QrCodeBuilder.() -> Unit): QrVerificationResult {
        val builder = QRCode.custom()
        builder.builderBlock()
        
        return try {
            val svg = builder.buildSvg(data)
            val qrResult = builder.build(data)
            val image = svgToBufferedImage(svg, qrResult.config.layout.width, qrResult.config.layout.height)
            
            val scanDuration = measureTime {
                val result = scanQrCode(image)
                return QrVerificationResult(
                    isSuccessful = true,
                    decodedContent = result.text,
                    expectedContent = data,
                    errorCorrection = result.resultMetadata?.get(ResultMetadataType.ERROR_CORRECTION_LEVEL) as? ErrorCorrectionLevel
                )
            }
            
            QrVerificationResult(isSuccessful = false, errorMessage = "Scan completed but no result returned")
        } catch (e: Exception) {
            QrVerificationResult(
                isSuccessful = false,
                expectedContent = data,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Performance testing for QR generation
     */
    fun measurePerformance(data: String, config: QrStyleConfig): QrPerformanceMetrics {
        var svg: String
        var qrResult: QrResult
        
        val generationTime = measureTime {
            val generator = DefaultQrGenerator()
            qrResult = generator.generateFromText(data, config)
        }
        
        val renderingTime = measureTime {
            val renderer = DefaultSvgRenderer()
            svg = renderer.render(qrResult)
        }
        
        val scanTime = measureTime {
            try {
                val image = svgToBufferedImage(svg, config.layout.width, config.layout.height)
                scanQrCode(image)
            } catch (e: Exception) {
                // Scan failed
            }
        }
        
        return QrPerformanceMetrics(
            generationTime = generationTime,
            svgSize = svg.length,
            renderingTime = renderingTime,
            scanTime = scanTime,
            isScannableByZXing = try {
                val image = svgToBufferedImage(svg, config.layout.width, config.layout.height)
                scanQrCode(image)
                true
            } catch (e: Exception) {
                false
            }
        )
    }
    
    /**
     * Batch verification of multiple QR codes
     */
    fun verifyBatch(testCases: List<Pair<String, QrStyleConfig>>): List<QrVerificationResult> {
        return testCases.map { (data, config) ->
            verify(data, config)
        }
    }
    
    /**
     * Test different styling options don't break scannability
     */
    fun testStylingCompatibility(data: String): Map<String, QrVerificationResult> {
        val testConfigs = mapOf(
            "default" to QrStyleConfig(),
            "circles" to QrStyleConfig(modules = ModuleOptions(type = DotType.CIRCLE)),
            "squares" to QrStyleConfig(modules = ModuleOptions(type = DotType.SQUARE)),
            "classy" to QrStyleConfig(modules = ModuleOptions(type = DotType.CLASSY)),
            "rounded" to QrStyleConfig(modules = ModuleOptions(type = DotType.ROUNDED)),
            "extra-rounded" to QrStyleConfig(modules = ModuleOptions(type = DotType.EXTRA_ROUNDED)),
            "with-logo" to QrStyleConfig(logo = LogoOptions(holeRadiusPx = 30.0)),
            "with-drop-shadow" to QrStyleConfig(advanced = AdvancedOptions(dropShadow = DropShadow(enabled = true))),
            "with-outline" to QrStyleConfig(advanced = AdvancedOptions(moduleOutline = ModuleOutline(enabled = true)))
        )
        
        return testConfigs.mapValues { (_, config) ->
            verify(data, config)
        }
    }
    
    private fun scanQrCode(image: BufferedImage): Result {
        val source = BufferedImageLuminanceSource(image)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return reader.decode(bitmap)
    }
    
    private fun svgToBufferedImage(svg: String, width: Int, height: Int): BufferedImage {
        // For now, create a simple black/white representation
        // In a real implementation, you'd use a proper SVG renderer like Batik
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()
        
        // Fill with white background
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, width, height)
        
        // Parse SVG and draw basic elements
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(svg.toByteArray()))
            
            // Simple parsing for circles and rectangles
            parseSvgElements(doc, g2d)
        } catch (e: Exception) {
            // Fallback: just draw a test pattern
            g2d.color = Color.BLACK
            g2d.fillRect(50, 50, width - 100, height - 100)
        }
        
        g2d.dispose()
        return image
    }
    
    private fun parseSvgElements(doc: Document, g2d: java.awt.Graphics2D) {
        g2d.color = Color.BLACK
        
        // Parse circles
        val circles = doc.getElementsByTagName("circle")
        for (i in 0 until circles.length) {
            val circle = circles.item(i)
            val attrs = circle.attributes
            val cx = attrs.getNamedItem("cx")?.nodeValue?.toDoubleOrNull() ?: 0.0
            val cy = attrs.getNamedItem("cy")?.nodeValue?.toDoubleOrNull() ?: 0.0
            val r = attrs.getNamedItem("r")?.nodeValue?.toDoubleOrNull() ?: 0.0
            
            g2d.fillOval((cx - r).toInt(), (cy - r).toInt(), (2 * r).toInt(), (2 * r).toInt())
        }
        
        // Parse rectangles
        val rects = doc.getElementsByTagName("rect")
        for (i in 0 until rects.length) {
            val rect = rects.item(i)
            val attrs = rect.attributes
            val x = attrs.getNamedItem("x")?.nodeValue?.toDoubleOrNull() ?: 0.0
            val y = attrs.getNamedItem("y")?.nodeValue?.toDoubleOrNull() ?: 0.0
            val width = attrs.getNamedItem("width")?.nodeValue?.toDoubleOrNull() ?: 0.0
            val height = attrs.getNamedItem("height")?.nodeValue?.toDoubleOrNull() ?: 0.0
            val fill = attrs.getNamedItem("fill")?.nodeValue
            
            if (fill != "none" && !fill.isNullOrEmpty()) {
                g2d.fillRect(x.toInt(), y.toInt(), width.toInt(), height.toInt())
            }
        }
        
        // Parse paths (for batched squares)
        val paths = doc.getElementsByTagName("path")
        for (i in 0 until paths.length) {
            val path = paths.item(i)
            val attrs = path.attributes
            val d = attrs.getNamedItem("d")?.nodeValue
            
            // Simple path parsing for rectangles (M x,y h w v h h-w z pattern)
            d?.let { pathData ->
                parseSimplePath(pathData, g2d)
            }
        }
    }
    
    private fun parseSimplePath(pathData: String, g2d: java.awt.Graphics2D) {
        // Very basic path parsing for rectangle patterns
        val commands = pathData.split("(?=[MmHhVvZz])".toRegex())
        
        var currentX = 0.0
        var currentY = 0.0
        
        for (command in commands) {
            if (command.trim().isEmpty()) continue
            
            when (command.trim().first()) {
                'M' -> {
                    val coords = command.substring(1).split(",")
                    if (coords.size >= 2) {
                        currentX = coords[0].trim().toDoubleOrNull() ?: 0.0
                        currentY = coords[1].trim().toDoubleOrNull() ?: 0.0
                    }
                }
                'h' -> {
                    val width = command.substring(1).trim().toDoubleOrNull() ?: 0.0
                    // Assume this is part of a rectangle pattern
                    // Look for following v and h commands to complete rectangle
                    g2d.fillRect(currentX.toInt(), currentY.toInt(), width.toInt(), width.toInt())
                }
            }
        }
    }
}

/** Utility functions for common test scenarios **/
object QrTestUtils {
    
    fun testAllModuleStyles(data: String = "Test QR Code"): Map<String, QrVerificationResult> {
        val verifier = QrVerifier()
        val results = mutableMapOf<String, QrVerificationResult>()
        
        DotType.values().forEach { dotType ->
            val config = QrStyleConfig(modules = ModuleOptions(type = dotType))
            results[dotType.name] = verifier.verify(data, config)
        }
        
        return results
    }
    
    fun testErrorCorrectionLevels(data: String = "Test QR Code"): Map<String, QrVerificationResult> {
        val verifier = QrVerifier()
        val results = mutableMapOf<String, QrVerificationResult>()
        
        listOf(
            io.nayuki.qrcodegen.QrCode.Ecc.LOW,
            io.nayuki.qrcodegen.QrCode.Ecc.MEDIUM,
            io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE,
            io.nayuki.qrcodegen.QrCode.Ecc.HIGH
        ).forEach { ecc ->
            val config = QrStyleConfig(qrOptions = QrOptions(ecc = ecc))
            results[ecc.name] = verifier.verify(data, config)
        }
        
        return results
    }
    
    fun benchmarkPerformance(iterations: Int = 100): List<QrPerformanceMetrics> {
        val verifier = QrVerifier()
        val results = mutableListOf<QrPerformanceMetrics>()
        
        repeat(iterations) { i ->
            val data = "Performance test iteration $i"
            val config = QrStyleConfig()
            results.add(verifier.measurePerformance(data, config))
        }
        
        return results
    }
} 