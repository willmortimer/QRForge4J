package io.github.qrgen.png

import io.github.qrgen.core.QrResult
import io.github.qrgen.core.RasterOptions
import io.github.qrgen.svg.DefaultSvgRenderer
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.JPEGTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import javax.imageio.IIOImage
import javax.imageio.ImageIO

/** PNG rendering configuration **/
data class PngRenderConfig(
    val dpi: Float = 300f,
    val quality: Float = 0.92f,
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

interface QrJpegRenderer {
    fun renderJpeg(qrResult: QrResult, config: RasterOptions = qrResult.config.raster): ByteArray
}

/** High-quality raster renderer using Apache Batik **/
class BatikPngRenderer(
    private val svgRenderer: DefaultSvgRenderer = DefaultSvgRenderer()
) : QrPngRenderer, QrJpegRenderer {

    override fun render(qrResult: QrResult, config: PngRenderConfig): ByteArray {
        val svg = svgRenderer.render(qrResult)
        val transcoder = PNGTranscoder().apply {
            addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 25.4f / config.dpi)
            addTranscodingHint(PNGTranscoder.KEY_EXECUTE_ONLOAD, true)
            if (config.colorMode == ColorMode.RGBA) {
                addTranscodingHint(PNGTranscoder.KEY_FORCE_TRANSPARENT_WHITE, true)
            }
        }
        return transcode(svg, transcoder)
    }

    override fun renderToFile(qrResult: QrResult, outputFile: File, config: PngRenderConfig) {
        outputFile.writeBytes(render(qrResult, config))
    }

    override fun renderToBufferedImage(qrResult: QrResult, config: PngRenderConfig): BufferedImage {
        return ImageIO.read(ByteArrayInputStream(render(qrResult, config)))
    }

    override fun renderJpeg(qrResult: QrResult, config: RasterOptions): ByteArray {
        val svg = svgRenderer.render(qrResult.ensureOpaqueBackground())
        val transcoder = JPEGTranscoder().apply {
            addTranscodingHint(JPEGTranscoder.KEY_QUALITY, config.jpegQuality.coerceIn(0.1f, 1.0f))
            addTranscodingHint(JPEGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, 25.4f / config.dpi)
        }
        return transcode(svg, transcoder)
    }

    private fun transcode(svg: String, transcoder: org.apache.batik.transcoder.image.ImageTranscoder): ByteArray {
        val input = TranscoderInput(StringReader(svg))
        val outputStream = ByteArrayOutputStream()
        val output = TranscoderOutput(outputStream)
        transcoder.transcode(input, output)
        return outputStream.toByteArray()
    }
}

/** Alternative Java2D-based PNG renderer for lighter dependencies **/
class Java2DPngRenderer(
    private val svgRenderer: DefaultSvgRenderer = DefaultSvgRenderer()
) : QrPngRenderer, QrJpegRenderer {

    override fun render(qrResult: QrResult, config: PngRenderConfig): ByteArray {
        val bufferedImage = renderToBufferedImage(qrResult, config)
        return writeImage(bufferedImage, "png", config.quality)
    }

    override fun renderToFile(qrResult: QrResult, outputFile: File, config: PngRenderConfig) {
        outputFile.writeBytes(render(qrResult, config))
    }

    override fun renderToBufferedImage(qrResult: QrResult, config: PngRenderConfig): BufferedImage {
        val svg = svgRenderer.render(qrResult)
        val imageType = if (config.colorMode == ColorMode.RGBA) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        val image = BufferedImage(qrResult.config.layout.width, qrResult.config.layout.height, imageType)
        val g2d = image.createGraphics()
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, image.width, image.height)
        g2d.dispose()
        return ImageIO.read(ByteArrayInputStream(BatikPngRenderer(svgRenderer).render(qrResult, config)))
            ?: image
    }

    override fun renderJpeg(qrResult: QrResult, config: RasterOptions): ByteArray {
        val image = renderToBufferedImage(qrResult.ensureOpaqueBackground(), PngRenderConfig(dpi = config.dpi))
        return writeImage(image, "jpg", config.jpegQuality)
    }

    private fun writeImage(image: BufferedImage, format: String, quality: Float): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val writer = ImageIO.getImageWritersByFormatName(format).next()
        val params = writer.defaultWriteParam
        if (params.canWriteCompressed()) {
            params.compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
            params.compressionQuality = quality.coerceIn(0.1f, 1.0f)
        }
        val imageOutput = ImageIO.createImageOutputStream(outputStream)
        writer.output = imageOutput
        writer.write(null, IIOImage(image, null, null), params)
        imageOutput.close()
        writer.dispose()
        return outputStream.toByteArray()
    }
}

private fun QrResult.ensureOpaqueBackground(): QrResult {
    return if (config.colors.background != null) this else copy(config = config.copy(colors = config.colors.copy(background = "#ffffff")))
}
