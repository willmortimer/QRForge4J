package io.github.qrgen.micronaut

import io.github.qrgen.core.*
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.serde.annotation.Serdeable
import kotlinx.coroutines.runBlocking

/**
 * Micronaut REST controller for QR code generation
 */
@Controller("/qr")
class QrController(private val qrGenService: QrGenService) {
    
    /**
     * Generate QR code with query parameters
     */
    @Get("/generate")
    fun generateQr(
        @QueryValue data: String,
        @QueryValue(defaultValue = "SVG") format: String,
        @QueryValue(defaultValue = "512") width: Int,
        @QueryValue(defaultValue = "512") height: Int,
        @QueryValue(required = false) foregroundColor: String? = null,
        @QueryValue(required = false) backgroundColor: String? = null
    ): HttpResponse<ByteArray> {
        val response = qrGenService.generateSimple(
            data = data,
            width = width,
            height = height,
            foregroundColor = foregroundColor ?: "#000000",
            backgroundColor = backgroundColor ?: "#ffffff",
            format = QrFormat.valueOf(format.uppercase())
        )
        
        return HttpResponse.ok(response.data)
            .contentType(MediaType.of(response.contentType))
            .header("Content-Disposition", "inline; filename=\"${response.filename}\"")
    }
    
    /**
     * Generate QR code with POST request
     */
    @Post("/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    fun generateQrPost(@Body request: QrRequest): HttpResponse<ByteArray> {
        val config = QrStyleConfig(
            layout = LayoutOptions(
                width = request.width,
                height = request.height,
                margin = request.margin
            ),
            colors = ColorOptions(
                foreground = request.foregroundColor,
                background = request.backgroundColor
            ),
            modules = ModuleOptions(
                type = DotType.valueOf(request.moduleType.uppercase())
            )
        )
        
        val response = qrGenService.generateQr(
            request.data,
            config,
            QrFormat.valueOf(request.format.uppercase())
        )
        
        return HttpResponse.ok(response.data)
            .contentType(MediaType.of(response.contentType))
            .header("Content-Disposition", "inline; filename=\"${response.filename}\"")
    }
    
    /**
     * Batch generate QR codes
     */
    @Post("/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun generateBatch(@Body request: BatchRequest): HttpResponse<BatchQrResponse> = runBlocking {
        val config = QrStyleConfig(
            layout = LayoutOptions(
                width = request.width,
                height = request.height
            ),
            colors = ColorOptions(
                foreground = request.foregroundColor,
                background = request.backgroundColor
            )
        )
        
        val response = qrGenService.generateBatch(
            request.dataList,
            config,
            QrFormat.valueOf(request.format.uppercase())
        )
        
        HttpResponse.ok(response)
    }
    
    /**
     * Health check endpoint
     */
    @Get("/health")
    @Produces(MediaType.APPLICATION_JSON)
    fun health(): HttpResponse<Map<String, Any>> {
        return HttpResponse.ok(
            mapOf(
                "status" to "UP",
                "service" to "QRGen",
                "version" to "1.0.0",
                "framework" to "Micronaut"
            )
        )
    }
}

/**
 * Request model for QR generation
 */
@Serdeable
data class QrRequest(
    val data: String,
    val format: String = "SVG",
    val width: Int = 512,
    val height: Int = 512,
    val margin: Int = 16,
    val foregroundColor: String = "#000000",
    val backgroundColor: String = "#ffffff",
    val moduleType: String = "CIRCLE"
)

/**
 * Request model for batch QR generation
 */
@Serdeable
data class BatchRequest(
    val dataList: List<String>,
    val format: String = "SVG",
    val width: Int = 512,
    val height: Int = 512,
    val foregroundColor: String = "#000000",
    val backgroundColor: String = "#ffffff"
) 