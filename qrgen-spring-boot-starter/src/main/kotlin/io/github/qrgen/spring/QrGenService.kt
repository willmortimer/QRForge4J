package io.github.qrgen.spring

import io.github.qrgen.batch.QrBatchProcessor
import io.github.qrgen.core.*
import io.github.qrgen.dsl.*
import io.github.qrgen.png.BatikPngRenderer
import io.github.qrgen.svg.DefaultSvgRenderer
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Spring service for QR code generation
 */
@Service
class QrGenService(
    private val qrGenerator: DefaultQrGenerator,
    private val svgRenderer: DefaultSvgRenderer,
    private val pngRenderer: BatikPngRenderer?,
    private val batchProcessor: QrBatchProcessor,
    private val properties: QrGenProperties
) {
    
    /**
     * Generate QR code with default configuration
     */
    fun generateQr(data: String, format: QrFormat = QrFormat.SVG): QrResponse {
        val config = createDefaultConfig()
        return generateQr(data, config, format)
    }
    
    /**
     * Generate QR code with custom configuration
     */
    fun generateQr(data: String, config: QrStyleConfig, format: QrFormat = QrFormat.SVG): QrResponse {
        validateInput(data)
        
        val qrResult = qrGenerator.generateFromText(data, config)
        
        return when (format) {
            QrFormat.SVG -> {
                val svg = svgRenderer.render(qrResult)
                QrResponse(
                    data = svg.toByteArray(),
                    contentType = "image/svg+xml",
                    filename = "qr.svg",
                    size = svg.length
                )
            }
            QrFormat.PNG -> {
                requireNotNull(pngRenderer) { "PNG renderer not available. Add qrgen-png dependency." }
                val png = pngRenderer.render(qrResult)
                QrResponse(
                    data = png,
                    contentType = "image/png",
                    filename = "qr.png",
                    size = png.size
                )
            }
        }
    }
    
    /**
     * Generate QR code using the DSL builder
     */
    fun generateQrWithBuilder(data: String, builderBlock: QrCodeBuilder.() -> Unit, format: QrFormat = QrFormat.SVG): QrResponse {
        validateInput(data)
        
        val builder = QRCode.custom()
        builder.builderBlock()
        
        return when (format) {
            QrFormat.SVG -> {
                val svg = builder.buildSvg(data)
                QrResponse(
                    data = svg.toByteArray(),
                    contentType = "image/svg+xml",
                    filename = "qr.svg",
                    size = svg.length
                )
            }
            QrFormat.PNG -> {
                requireNotNull(pngRenderer) { "PNG renderer not available. Add qrgen-png dependency." }
                val qrResult = builder.build(data)
                val png = pngRenderer.render(qrResult)
                QrResponse(
                    data = png,
                    contentType = "image/png",
                    filename = "qr.png",
                    size = png.size
                )
            }
        }
    }
    
    /**
     * Batch generate QR codes
     */
    suspend fun generateBatch(
        dataList: List<String>,
        config: QrStyleConfig? = null,
        format: QrFormat = QrFormat.SVG
    ): BatchQrResponse {
        validateBatchInput(dataList)
        
        val effectiveConfig = config ?: createDefaultConfig()
        val batchConfig = io.github.qrgen.batch.BatchConfig(
            parallelism = properties.batch.parallelism,
            memoryThresholdMB = properties.batch.memoryThresholdMB,
            outputFormat = when (format) {
                QrFormat.SVG -> io.github.qrgen.batch.OutputFormat.SVG
                QrFormat.PNG -> io.github.qrgen.batch.OutputFormat.PNG
            }
        )
        
        val processor = QrBatchProcessor(batchConfig)
        val result = processor.processBatch(dataList, effectiveConfig)
        
        return BatchQrResponse(
            totalProcessed = result.totalProcessed,
            successful = result.successful,
            failed = result.failed,
            processingTimeMs = result.processingTime.inWholeMilliseconds,
            averageTimePerQrMs = result.averageTimePerQr.inWholeMilliseconds,
            errors = result.errors.map { BatchError(it.index, it.data, it.error.message ?: "Unknown error") }
        )
    }
    
    private fun createDefaultConfig(): QrStyleConfig {
        val defaults = properties.defaults
        return QrStyleConfig(
            layout = LayoutOptions(
                width = defaults.width,
                height = defaults.height,
                margin = defaults.margin
            ),
            colors = ColorOptions(
                foreground = defaults.foregroundColor,
                background = defaults.backgroundColor
            ),
            qrOptions = QrOptions(
                ecc = when (defaults.errorCorrection.uppercase()) {
                    "LOW", "L" -> io.nayuki.qrcodegen.QrCode.Ecc.LOW
                    "MEDIUM", "M" -> io.nayuki.qrcodegen.QrCode.Ecc.MEDIUM
                    "QUARTILE", "Q" -> io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
                    "HIGH", "H" -> io.nayuki.qrcodegen.QrCode.Ecc.HIGH
                    else -> io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
                }
            )
        )
    }
    
    private fun validateInput(data: String) {
        require(data.isNotBlank()) { "QR data cannot be blank" }
        require(data.length <= properties.web.maxDataLength) { 
            "QR data too long: ${data.length} > ${properties.web.maxDataLength}" 
        }
    }
    
    private fun validateBatchInput(dataList: List<String>) {
        require(dataList.isNotEmpty()) { "Batch data cannot be empty" }
        require(dataList.size <= properties.batch.maxBatchSize) { 
            "Batch size too large: ${dataList.size} > ${properties.batch.maxBatchSize}" 
        }
        dataList.forEach { validateInput(it) }
    }
}

/**
 * QR code response data
 */
data class QrResponse(
    val data: ByteArray,
    val contentType: String,
    val filename: String,
    val size: Int
) {
    fun toInputStream(): InputStream = ByteArrayInputStream(data)
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as QrResponse
        
        if (!data.contentEquals(other.data)) return false
        if (contentType != other.contentType) return false
        if (filename != other.filename) return false
        if (size != other.size) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + size
        return result
    }
}

/**
 * Batch QR generation response
 */
data class BatchQrResponse(
    val totalProcessed: Int,
    val successful: Int,
    val failed: Int,
    val processingTimeMs: Long,
    val averageTimePerQrMs: Long,
    val errors: List<BatchError>
)

data class BatchError(
    val index: Int,
    val data: String,
    val errorMessage: String
)

enum class QrFormat { SVG, PNG } 