package io.github.qrgen.micronaut

import io.github.qrgen.batch.BatchConfig
import io.github.qrgen.batch.OutputFormat
import io.github.qrgen.batch.QrBatchProcessor
import io.github.qrgen.core.*
import io.github.qrgen.pdf.QrPdfRenderer
import io.github.qrgen.png.BatikPngRenderer
import io.github.qrgen.svg.DefaultSvgRenderer
import jakarta.inject.Singleton

@Singleton
class QrGenService {
    private val generator = DefaultQrGenerator()
    private val svgRenderer = DefaultSvgRenderer()
    private val pngRenderer = BatikPngRenderer()
    private val pdfRenderer = QrPdfRenderer()
    private val cache = QrRenderCache(CacheOptions(enabled = true, maxEntries = 256))

    fun generateQr(
        data: String,
        config: QrStyleConfig = QrStyleConfig(),
        format: QrFormat = QrFormat.SVG
    ): QrGenResponse {
        val bytes = cache.getOrPut(qrCacheKey(data, config, format.name)) {
            val qrResult = generator.generateFromText(data, config)
            when (format) {
                QrFormat.SVG -> svgRenderer.render(qrResult).toByteArray()
                QrFormat.PNG -> pngRenderer.render(qrResult)
                QrFormat.JPEG -> pngRenderer.renderJpeg(qrResult)
                QrFormat.PDF -> pdfRenderer.render(qrResult)
            }
        }

        return when (format) {
            QrFormat.SVG -> QrGenResponse(bytes, "image/svg+xml", "qr.svg", bytes.size)
            QrFormat.PNG -> QrGenResponse(bytes, "image/png", "qr.png", bytes.size)
            QrFormat.JPEG -> QrGenResponse(bytes, "image/jpeg", "qr.jpg", bytes.size)
            QrFormat.PDF -> QrGenResponse(bytes, "application/pdf", "qr.pdf", bytes.size)
        }
    }

    fun generateQr(request: QrGenerateRequest): QrGenResponse {
        return generateQr(request.data, QrRequestMapper.toConfig(request), QrFormat.valueOf(request.format.uppercase()))
    }

    suspend fun generateBatch(
        dataList: List<String>,
        config: QrGenerateRequest
    ): BatchQrResponse {
        val format = QrFormat.valueOf(config.format.uppercase())
        val processor = QrBatchProcessor(
            BatchConfig(
                outputFormat = when (format) {
                    QrFormat.SVG -> OutputFormat.SVG
                    QrFormat.PNG -> OutputFormat.PNG
                    QrFormat.JPEG -> OutputFormat.JPEG
                    QrFormat.PDF -> OutputFormat.PDF
                }
            )
        )
        val result = processor.processBatch(dataList, QrRequestMapper.toConfig(config))
        return BatchQrResponse(
            totalCount = dataList.size,
            successCount = result.successful,
            failureCount = result.failed,
            format = format.name
        )
    }

    fun generateSimple(
        data: String,
        width: Int = 512,
        height: Int = 512,
        foregroundColor: String = "#000000",
        backgroundColor: String = "#ffffff",
        format: QrFormat = QrFormat.SVG
    ): QrGenResponse {
        val request = QrGenerateRequest(
            data = data,
            width = width,
            height = height,
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor,
            format = format.name
        )
        return generateQr(request)
    }
}

data class QrGenResponse(
    val data: ByteArray,
    val contentType: String,
    val filename: String,
    val size: Int
)

data class BatchQrResponse(
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val format: String
)

enum class QrFormat { SVG, PNG, JPEG, PDF }
