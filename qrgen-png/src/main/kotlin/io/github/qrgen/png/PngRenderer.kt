package io.github.qrgen.png

import io.github.qrgen.core.QrResult
import io.github.qrgen.svg.DefaultSvgRenderer
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.awt.image.BufferedImage
import java.io.*
import javax.imageio.ImageIO

/** PNG rendering configuration **/
data class PngRenderConfig(
    val dpi: Float = 300f,
    val quality: Float = 1.0f,
    val antiAliasing: Boolean = true,
    val colorMode: ColorMode = ColorMode.RGB,
    val compression: CompressionLevel = CompressionLevel.BALANCED
)

enum class ColorMode { RGB, RGBA, GRAYSCALE }
enum class CompressionLevel { NONE, FAST, BALANCED, MAXIMUM }

/** PNG renderer interface **/
interface QrPngRenderer {
    fun render(qrResult: QrResult, config: PngRenderConfig = PngRenderConfig()): ByteArray
    fun renderToFile(qrResult: QrResult, outputFile: File, config: PngRenderConfig = PngRenderConfig())
    fun renderToBufferedImage(qrResult: QrResult, config: PngRenderConfig = PngRenderConfig()): BufferedImage
}

/** High-quality PNG renderer using Apache Batik **/
class BatikPngRenderer(
    private val svgRenderer: DefaultSvgRenderer = DefaultSvgRenderer()
) : QrPngRenderer {
    
    override fun render(qrResult: QrResult, config: PngRenderConfig): ByteArray {
        val svg = svgRenderer.render(qrResult)
        return svgToPng(svg, config)
    }
    
    override fun renderToFile(qrResult: QrResult, outputFile: File, config: PngRenderConfig) {
        val pngData = render(qrResult, config)
        outputFile.writeBytes(pngData)
    }
    
    override fun renderToBufferedImage(qrResult: QrResult, config: PngRenderConfig): BufferedImage {
        val pngData = render(qrResult, config)
        return ImageIO.read(ByteArrayInputStream(pngData))
    }
    
    /**
     * Convert SVG to PNG using Batik transcoder
     */
    private fun svgToPng(svg: String, config: PngRenderConfig): ByteArray {
        val transcoder = PNGTranscoder()
        
        // Configure transcoder settings
        configureBatikTranscoder(transcoder, config)
        
        // Create input and output
        val input = TranscoderInput(StringReader(svg))
        val outputStream = ByteArrayOutputStream()
        val output = TranscoderOutput(outputStream)
        
        // Perform transcoding
        transcoder.transcode(input, output)
        
        return outputStream.toByteArray()
    }
    
    private fun configureBatikTranscoder(transcoder: PNGTranscoder, config: PngRenderConfig) {
        // Set DPI for high-resolution output
        transcoder.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 25.4f / config.dpi)
        
        // Anti-aliasing
        if (config.antiAliasing) {
            transcoder.addTranscodingHint(PNGTranscoder.KEY_FORCE_TRANSPARENT_WHITE, false)
        }
        
        // Color mode configuration
        when (config.colorMode) {
            ColorMode.RGBA -> {
                // Enable alpha channel
                transcoder.addTranscodingHint(PNGTranscoder.KEY_FORCE_TRANSPARENT_WHITE, true)
            }
            ColorMode.GRAYSCALE -> {
                // Batik doesn't directly support grayscale, but we can post-process
            }
            else -> {
                // RGB is default
            }
        }
        
        // Quality settings
        transcoder.addTranscodingHint(PNGTranscoder.KEY_GAMMA, 2.2f)
        
        // Memory optimization
        transcoder.addTranscodingHint(PNGTranscoder.KEY_EXECUTE_ONLOAD, true)
    }
}

/** Alternative Java2D-based PNG renderer for lighter dependencies **/
class Java2DPngRenderer(
    private val svgRenderer: DefaultSvgRenderer = DefaultSvgRenderer()
) : QrPngRenderer {
    
    override fun render(qrResult: QrResult, config: PngRenderConfig): ByteArray {
        val bufferedImage = renderToBufferedImage(qrResult, config)
        val outputStream = ByteArrayOutputStream()
        
        // Configure ImageIO for PNG output
        val writers = ImageIO.getImageWritersByFormatName("PNG")
        val writer = writers.next()
        val writeParam = writer.defaultWriteParam
        
        // Configure compression
        when (config.compression) {
            CompressionLevel.NONE -> {
                writeParam.compressionMode = javax.imageio.ImageWriteParam.MODE_DISABLED
            }
            CompressionLevel.FAST -> {
                writeParam.compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                writeParam.compressionQuality = 0.8f
            }
            CompressionLevel.BALANCED -> {
                writeParam.compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                writeParam.compressionQuality = 0.9f
            }
            CompressionLevel.MAXIMUM -> {
                writeParam.compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                writeParam.compressionQuality = 1.0f
            }
        }
        
        val imageOutputStream = ImageIO.createImageOutputStream(outputStream)
        writer.output = imageOutputStream
        writer.write(null, javax.imageio.IIOImage(bufferedImage, null, null), writeParam)
        
        writer.dispose()
        imageOutputStream.close()
        
        return outputStream.toByteArray()
    }
    
    override fun renderToFile(qrResult: QrResult, outputFile: File, config: PngRenderConfig) {
        val bufferedImage = renderToBufferedImage(qrResult, config)
        ImageIO.write(bufferedImage, "PNG", outputFile)
    }
    
    override fun renderToBufferedImage(qrResult: QrResult, config: PngRenderConfig): BufferedImage {
        val svg = svgRenderer.render(qrResult)
        
        // Calculate dimensions based on DPI
        val layout = qrResult.config.layout
        val scale = config.dpi / 72f // 72 DPI is default
        val width = (layout.width * scale).toInt()
        val height = (layout.height * scale).toInt()
        
        // Create high-resolution image
        val imageType = when (config.colorMode) {
            ColorMode.RGBA -> BufferedImage.TYPE_INT_ARGB
            ColorMode.GRAYSCALE -> BufferedImage.TYPE_BYTE_GRAY
            else -> BufferedImage.TYPE_INT_RGB
        }
        
        val image = BufferedImage(width, height, imageType)
        val g2d = image.createGraphics()
        
        // Configure high-quality rendering
        if (config.antiAliasing) {
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        }
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY)
        
        // Scale graphics context
        g2d.scale(scale.toDouble(), scale.toDouble())
        
        // Simple SVG rendering (for production, use proper SVG renderer)
        renderSimpleSvg(svg, g2d, qrResult.config.layout.width, qrResult.config.layout.height)
        
        g2d.dispose()
        return image
    }
    
    private fun renderSimpleSvg(svg: String, g2d: java.awt.Graphics2D, width: Int, height: Int) {
        // This is a simplified SVG renderer
        // For production use, integrate with Batik's SVG DOM or use the BatikPngRenderer
        
        // Parse basic background
        if (svg.contains("fill=\"#")) {
            val bgColorMatch = Regex("""<rect[^>]*fill="([^"]*)"[^>]*width="$width"[^>]*height="$height"""").find(svg)
            bgColorMatch?.let { match ->
                val colorStr = match.groupValues[1]
                if (colorStr != "none") {
                    try {
                        val color = java.awt.Color.decode(colorStr)
                        g2d.color = color
                        g2d.fillRect(0, 0, width, height)
                    } catch (e: Exception) {
                        // Default to white background
                        g2d.color = java.awt.Color.WHITE
                        g2d.fillRect(0, 0, width, height)
                    }
                }
            }
        }
        
        // Parse and render circles
        val circlePattern = Regex("""<circle[^>]*cx="([^"]*)"[^>]*cy="([^"]*)"[^>]*r="([^"]*)"[^>]*fill="([^"]*)"[^>]*/>""")
        circlePattern.findAll(svg).forEach { match ->
            val cx = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val cy = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val r = match.groupValues[3].toDoubleOrNull() ?: 0.0
            val fillColor = match.groupValues[4]
            
            if (fillColor != "none" && fillColor.isNotEmpty()) {
                try {
                    g2d.color = java.awt.Color.decode(fillColor)
                    val diameter = (2 * r).toInt()
                    g2d.fillOval((cx - r).toInt(), (cy - r).toInt(), diameter, diameter)
                } catch (e: Exception) {
                    // Skip invalid colors
                }
            }
        }
        
        // Parse and render rectangles
        val rectPattern = Regex("""<rect[^>]*x="([^"]*)"[^>]*y="([^"]*)"[^>]*width="([^"]*)"[^>]*height="([^"]*)"[^>]*fill="([^"]*)"[^>]*/>""")
        rectPattern.findAll(svg).forEach { match ->
            val x = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val y = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val w = match.groupValues[3].toDoubleOrNull() ?: 0.0
            val h = match.groupValues[4].toDoubleOrNull() ?: 0.0
            val fillColor = match.groupValues[5]
            
            if (fillColor != "none" && fillColor.isNotEmpty()) {
                try {
                    g2d.color = java.awt.Color.decode(fillColor)
                    g2d.fillRect(x.toInt(), y.toInt(), w.toInt(), h.toInt())
                } catch (e: Exception) {
                    // Skip invalid colors
                }
            }
        }
    }
}

/** Utility object for PNG rendering **/
object PngUtils {
    
    /**
     * Create a high-DPI PNG suitable for print
     */
    fun createPrintQuality(qrResult: QrResult): ByteArray {
        val renderer = BatikPngRenderer()
        val config = PngRenderConfig(
            dpi = 300f,
            quality = 1.0f,
            antiAliasing = true,
            compression = CompressionLevel.MAXIMUM
        )
        return renderer.render(qrResult, config)
    }
    
    /**
     * Create a web-optimized PNG
     */
    fun createWebOptimized(qrResult: QrResult): ByteArray {
        val renderer = BatikPngRenderer()
        val config = PngRenderConfig(
            dpi = 96f,
            quality = 0.85f,
            antiAliasing = true,
            compression = CompressionLevel.BALANCED
        )
        return renderer.render(qrResult, config)
    }
    
    /**
     * Create a retina-display PNG (2x resolution)
     */
    fun createRetina(qrResult: QrResult): ByteArray {
        val renderer = BatikPngRenderer()
        val config = PngRenderConfig(
            dpi = 192f, // 2x standard 96 DPI
            quality = 0.9f,
            antiAliasing = true,
            compression = CompressionLevel.BALANCED
        )
        return renderer.render(qrResult, config)
    }
} 