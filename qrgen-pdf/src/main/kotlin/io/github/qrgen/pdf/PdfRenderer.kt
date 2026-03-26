package io.github.qrgen.pdf

import io.github.qrgen.core.QrResult
import io.github.qrgen.png.BatikPngRenderer
import io.github.qrgen.png.PngRenderConfig
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayOutputStream
import java.io.File

class QrPdfRenderer(
    private val pngRenderer: BatikPngRenderer = BatikPngRenderer()
) {
    fun render(qrResult: QrResult): ByteArray {
        val png = pngRenderer.render(qrResult, PngRenderConfig(dpi = qrResult.config.raster.dpi))
        PDDocument().use { document ->
            val page = PDPage(PDRectangle(qrResult.config.layout.width.toFloat(), qrResult.config.layout.height.toFloat()))
            document.addPage(page)
            val image = PDImageXObject.createFromByteArray(document, png, "qrgen")
            PDPageContentStream(document, page).use { content ->
                content.drawImage(image, 0f, 0f, qrResult.config.layout.width.toFloat(), qrResult.config.layout.height.toFloat())
            }
            val output = ByteArrayOutputStream()
            document.save(output)
            return output.toByteArray()
        }
    }

    fun renderToFile(qrResult: QrResult, file: File) {
        file.writeBytes(render(qrResult))
    }

    fun isReadable(bytes: ByteArray): Boolean = runCatching { Loader.loadPDF(bytes).use { } }.isSuccess
}
