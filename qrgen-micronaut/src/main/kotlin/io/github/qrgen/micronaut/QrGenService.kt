package io.github.qrgen.micronaut

import io.github.qrgen.core.*
import io.github.qrgen.svg.SvgRenderer
import io.github.qrgen.png.PngRenderer
import io.github.qrgen.batch.BatchProcessor
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

/**
 * Micronaut service for QR code generation
 */
@Singleton
class QrGenService {
    
    private val svgRenderer = SvgRenderer()
    private val pngRenderer = PngRenderer()
    private val batchProcessor = BatchProcessor()
    
    /**
     * Generate a QR code with the specified configuration
     */
    fun generateQr(
        data: String, 
        config: QrStyleConfig = QrStyleConfig(), 
        format: QrFormat = QrFormat.SVG
    ): QrGenResponse {
        return when (format) {
            QrFormat.SVG -> {
                val svg = svgRenderer.render(data, config)
                QrGenResponse(
                    data = svg.toByteArray(),
                    contentType = "image/svg+xml",
                    filename = "qr.svg",
                    size = svg.length
                )
            }
            QrFormat.PNG -> {
                val png = pngRenderer.render(data, config)
                QrGenResponse(
                    data = png,
                    contentType = "image/png",
                    filename = "qr.png",
                    size = png.size
                )
            }
        }
    }
    
    /**
     * Generate multiple QR codes in batch
     */
    suspend fun generateBatch(
        dataList: List<String>,
        config: QrStyleConfig = QrStyleConfig(),
        format: QrFormat = QrFormat.SVG
    ): BatchQrResponse {
        val responses = batchProcessor.processBatch(dataList, config, format)
        return BatchQrResponse(
            results = responses,
            totalCount = dataList.size,
            successCount = responses.size,
            format = format.name
        )
    }
    
    /**
     * Generate QR code with simple parameters
     */
    fun generateSimple(
        data: String,
        width: Int = 512,
        height: Int = 512,
        foregroundColor: String = "#000000",
        backgroundColor: String = "#ffffff",
        format: QrFormat = QrFormat.SVG
    ): QrGenResponse {
        val config = QrStyleConfig(
            layout = LayoutOptions(width = width, height = height),
            colors = ColorOptions(
                foreground = foregroundColor,
                background = backgroundColor
            )
        )
        return generateQr(data, config, format)
    }
}

/**
 * Response model for QR generation
 */
data class QrGenResponse(
    val data: ByteArray,
    val contentType: String,
    val filename: String,
    val size: Int
)

/**
 * Response model for batch QR generation
 */
data class BatchQrResponse(
    val results: List<QrGenResponse>,
    val totalCount: Int,
    val successCount: Int,
    val format: String
) 