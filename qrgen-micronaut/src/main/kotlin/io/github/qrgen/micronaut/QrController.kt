package io.github.qrgen.micronaut

import io.github.qrgen.core.QrGenerateRequest
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*

@Controller("/qr")
class QrController(private val qrGenService: QrGenService) {

    @Get("/generate")
    fun generateQr(
        @QueryValue data: String,
        @QueryValue(defaultValue = "SVG") format: String,
        @QueryValue(defaultValue = "512") width: Int,
        @QueryValue(defaultValue = "512") height: Int,
        @QueryValue foregroundColor: String? = null,
        @QueryValue backgroundColor: String? = null,
        @QueryValue cornerStyle: String? = null,
        @QueryValue cornerLogo: String? = null,
        @QueryValue alignmentPatternShape: String? = null,
        @QueryValue animationPreset: String? = null
    ): HttpResponse<ByteArray> {
        val request = QrGenerateRequest(
            data = data,
            format = format,
            width = width,
            height = height,
            foregroundColor = foregroundColor ?: "#000000",
            backgroundColor = backgroundColor ?: "#ffffff",
            cornerStyle = cornerStyle,
            cornerLogo = cornerLogo,
            alignmentPatternShape = alignmentPatternShape,
            animationPreset = animationPreset
        )
        return response(qrGenService.generateQr(request))
    }

    @Post("/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    fun generateQrPost(@Body request: QrGenerateRequest): HttpResponse<ByteArray> {
        return response(qrGenService.generateQr(request))
    }

    @Post("/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun generateBatch(@Body request: BatchRequest): HttpResponse<BatchQrResponse> {
        return HttpResponse.ok(qrGenService.generateBatch(request.dataList, request.config))
    }

    @Get("/health")
    @Produces(MediaType.APPLICATION_JSON)
    fun health(): HttpResponse<Map<String, Any>> {
        return HttpResponse.ok(
            mapOf(
                "status" to "UP",
                "service" to "QRGen",
                "framework" to "Micronaut",
                "formats" to QrFormat.entries.map { it.name }
            )
        )
    }

    private fun response(response: QrGenResponse): HttpResponse<ByteArray> {
        return HttpResponse.ok(response.data)
            .contentType(MediaType.of(response.contentType))
            .header("Content-Disposition", "inline; filename=\"${response.filename}\"")
    }
}

@Introspected
data class BatchRequest(
    val dataList: List<String>,
    val config: QrGenerateRequest = QrGenerateRequest(data = "")
)
