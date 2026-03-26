package io.github.qrgen.spring

import io.github.qrgen.core.QrGenerateRequest
import io.github.qrgen.core.QrRequestMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlinx.coroutines.runBlocking

@RestController
@RequestMapping("\${qrgen.web.base-path:/qr}")
@ConditionalOnProperty(prefix = "qrgen.web", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@CrossOrigin(origins = ["\${qrgen.web.cors-origins:*}"])
class QrController(private val qrGenService: QrGenService) {

    @GetMapping("/generate")
    fun generateQr(
        @RequestParam data: String,
        @RequestParam(defaultValue = "SVG") format: String,
        @RequestParam(defaultValue = "512") width: Int,
        @RequestParam(defaultValue = "512") height: Int,
        @RequestParam(required = false) foregroundColor: String?,
        @RequestParam(required = false) backgroundColor: String?,
        @RequestParam(required = false) moduleType: String?,
        @RequestParam(required = false) cornerStyle: String?,
        @RequestParam(required = false) cornerLogo: String?,
        @RequestParam(required = false) alignmentPatternShape: String?,
        @RequestParam(required = false) animationPreset: String?
    ): ResponseEntity<ByteArray> {
        val request = QrGenerateRequest(
            data = data,
            format = format,
            width = width,
            height = height,
            foregroundColor = foregroundColor ?: "#000000",
            backgroundColor = backgroundColor ?: "#ffffff",
            moduleType = moduleType,
            cornerStyle = cornerStyle,
            cornerLogo = cornerLogo,
            alignmentPatternShape = alignmentPatternShape,
            animationPreset = animationPreset
        )
        return response(qrGenService.generateQr(request))
    }

    @PostMapping("/generate")
    fun generateQrPost(@RequestBody request: QrGenerateRequest): ResponseEntity<ByteArray> {
        return response(qrGenService.generateQr(request))
    }

    @PostMapping("/batch")
    fun generateBatch(@RequestBody request: QrBatchRequest): ResponseEntity<BatchQrResponse> = runBlocking {
        ResponseEntity.ok(qrGenService.generateBatch(request.dataList, QrRequestMapper.toConfig(request.config), QrFormat.valueOf(request.config.format.uppercase())))
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "QRGen",
                "version" to "1.0.0",
                "formats" to QrFormat.entries.map { it.name }
            )
        )
    }

    private fun response(response: QrResponse): ResponseEntity<ByteArray> {
        val headers = HttpHeaders().apply {
            set("Content-Type", response.contentType)
            set("Content-Disposition", "inline; filename=\"${response.filename}\"")
            set("Content-Length", response.size.toString())
        }
        return ResponseEntity.ok().headers(headers).body(response.data)
    }
}

data class QrBatchRequest(
    val dataList: List<String>,
    val config: QrGenerateRequest = QrGenerateRequest(data = "")
)
