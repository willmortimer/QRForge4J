package io.github.qrgen.spring

import io.github.qrgen.core.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlinx.coroutines.runBlocking

/**
 * REST controller for QR code generation endpoints
 */
@RestController
@RequestMapping("\${qrgen.web.base-path:/qr}")
@ConditionalOnProperty(prefix = "qrgen.web", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@CrossOrigin(origins = ["\${qrgen.web.cors-origins:*}"])
class QrController(private val qrGenService: QrGenService) {
    
    /**
     * Generate QR code with query parameters
     */
    @GetMapping("/generate")
    fun generateQr(
        @RequestParam data: String,
        @RequestParam(defaultValue = "SVG") format: String,
        @RequestParam(defaultValue = "512") width: Int,
        @RequestParam(defaultValue = "512") height: Int,
        @RequestParam(required = false) foregroundColor: String?,
        @RequestParam(required = false) backgroundColor: String?,
        @RequestParam(defaultValue = "QUARTILE") errorCorrection: String
    ): ResponseEntity<ByteArray> {
        val qrFormat = QrFormat.valueOf(format.uppercase())
        
        val config = QrStyleConfig(
            layout = LayoutOptions(width = width, height = height),
            colors = ColorOptions(
                foreground = foregroundColor ?: "#000000",
                background = backgroundColor ?: "#ffffff"
            ),
            qrOptions = QrOptions(
                ecc = when (errorCorrection.uppercase()) {
                    "LOW", "L" -> io.nayuki.qrcodegen.QrCode.Ecc.LOW
                    "MEDIUM", "M" -> io.nayuki.qrcodegen.QrCode.Ecc.MEDIUM
                    "QUARTILE", "Q" -> io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
                    "HIGH", "H" -> io.nayuki.qrcodegen.QrCode.Ecc.HIGH
                    else -> io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
                }
            )
        )
        
        val response = qrGenService.generateQr(data, config, qrFormat)
        
        val headers = HttpHeaders().apply {
            set("Content-Type", response.contentType)
            set("Content-Disposition", "inline; filename=\"${response.filename}\"")
            set("Content-Length", response.size.toString())
        }
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(response.data)
    }
    
    /**
     * Generate QR code with POST request and JSON configuration
     */
    @PostMapping("/generate")
    fun generateQrPost(@RequestBody request: QrGenerateRequest): ResponseEntity<ByteArray> {
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
            modules = request.moduleType?.let { 
                ModuleOptions(type = DotType.valueOf(it.uppercase())) 
            } ?: ModuleOptions(),
            locators = request.cornerStyle?.let { 
                LocatorOptions(shape = parseLocatorShape(it)) 
            } ?: LocatorOptions(),
            qrOptions = QrOptions(
                ecc = when (request.errorCorrection.uppercase()) {
                    "LOW", "L" -> io.nayuki.qrcodegen.QrCode.Ecc.LOW
                    "MEDIUM", "M" -> io.nayuki.qrcodegen.QrCode.Ecc.MEDIUM
                    "QUARTILE", "Q" -> io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
                    "HIGH", "H" -> io.nayuki.qrcodegen.QrCode.Ecc.HIGH
                    else -> io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
                }
            )
        )
        
        val response = qrGenService.generateQr(request.data, config, QrFormat.valueOf(request.format.uppercase()))
        
        val headers = HttpHeaders().apply {
            set("Content-Type", response.contentType)
            set("Content-Disposition", "inline; filename=\"${response.filename}\"")
            set("Content-Length", response.size.toString())
        }
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(response.data)
    }
    
    /**
     * Batch generate QR codes
     */
    @PostMapping("/batch")
    fun generateBatch(@RequestBody request: QrBatchRequest): ResponseEntity<BatchQrResponse> = runBlocking {
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
            qrOptions = QrOptions(
                ecc = when (request.errorCorrection.uppercase()) {
                    "LOW", "L" -> io.nayuki.qrcodegen.QrCode.Ecc.LOW
                    "MEDIUM", "M" -> io.nayuki.qrcodegen.QrCode.Ecc.MEDIUM
                    "QUARTILE", "Q" -> io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
                    "HIGH", "H" -> io.nayuki.qrcodegen.QrCode.Ecc.HIGH
                    else -> io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
                }
            )
        )
        
        val response = qrGenService.generateBatch(
            request.dataList,
            config,
            QrFormat.valueOf(request.format.uppercase())
        )
        
        ResponseEntity.ok(response)
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "QRGen",
                "version" to "1.0.0",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Generate a simple QR code for testing
     */
    @GetMapping("/test")
    fun testQr(): ResponseEntity<ByteArray> {
        val response = qrGenService.generateQr("Hello, QRGen!", QrFormat.SVG)
        
        val headers = HttpHeaders().apply {
            set("Content-Type", response.contentType)
            set("Content-Disposition", "inline; filename=\"test.svg\"")
        }
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(response.data)
    }
    
    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleValidationError(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest()
            .body(ErrorResponse("VALIDATION_ERROR", ex.message ?: "Invalid request"))
    }
    
    /**
     * Exception handler for general errors
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneralError(ex: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", "An error occurred while generating QR code"))
    }

    /**
     * Parse corner style string to LocatorShape
     */
    private fun parseLocatorShape(style: String): LocatorShape {
        return when (style.uppercase()) {
            "SQUARE" -> LocatorShape.Square
            "CIRCLE" -> LocatorShape.Circle
            "ROUNDED" -> LocatorShape.Rounded()
            "CLASSY" -> LocatorShape.Classy
            else -> LocatorShape.Square
        }
    }
}

/**
 * Request model for QR generation
 */
data class QrGenerateRequest(
    val data: String,
    val format: String = "SVG",
    val width: Int = 512,
    val height: Int = 512,
    val margin: Int = 16,
    val foregroundColor: String = "#000000",
    val backgroundColor: String = "#ffffff",
    val errorCorrection: String = "QUARTILE",
    val moduleType: String? = null,
    val cornerStyle: String? = null
)

/**
 * Request model for batch QR generation
 */
data class QrBatchRequest(
    val dataList: List<String>,
    val format: String = "SVG",
    val width: Int = 512,
    val height: Int = 512,
    val margin: Int = 16,
    val foregroundColor: String = "#000000",
    val backgroundColor: String = "#ffffff",
    val errorCorrection: String = "QUARTILE"
)

/**
 * Error response model
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) 