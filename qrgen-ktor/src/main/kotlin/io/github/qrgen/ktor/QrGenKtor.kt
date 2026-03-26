package io.github.qrgen.ktor

import io.github.qrgen.core.DefaultQrGenerator
import io.github.qrgen.core.QrGenerateRequest
import io.github.qrgen.core.QrProfileRegistry
import io.github.qrgen.core.QrRenderCache
import io.github.qrgen.core.QrRequestMapper
import io.github.qrgen.core.QrStyleConfig
import io.github.qrgen.core.qrCacheKey
import io.github.qrgen.pdf.QrPdfRenderer
import io.github.qrgen.png.BatikPngRenderer
import io.github.qrgen.svg.DefaultSvgRenderer
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

class QrGenKtorService(
    private val generator: DefaultQrGenerator = DefaultQrGenerator(),
    private val svgRenderer: DefaultSvgRenderer = DefaultSvgRenderer(),
    private val pngRenderer: BatikPngRenderer = BatikPngRenderer(),
    private val pdfRenderer: QrPdfRenderer = QrPdfRenderer(),
    private val profiles: QrProfileRegistry = QrProfileRegistry(),
    private val cache: QrRenderCache = QrRenderCache()
) {
    fun generateQr(
        data: String,
        config: QrStyleConfig = QrStyleConfig(),
        format: QrKtorFormat = QrKtorFormat.SVG
    ): QrKtorResponse {
        val key = qrCacheKey(data, config, format.name)
        val bytes = cache.getOrPut(key) {
            val qrResult = generator.generateFromText(data, config)
            when (format) {
                QrKtorFormat.SVG -> svgRenderer.render(qrResult).toByteArray()
                QrKtorFormat.PNG -> pngRenderer.render(qrResult)
                QrKtorFormat.JPEG -> pngRenderer.renderJpeg(qrResult)
                QrKtorFormat.PDF -> pdfRenderer.render(qrResult)
            }
        }
        return when (format) {
            QrKtorFormat.SVG -> QrKtorResponse(bytes, ContentType.Image.SVG, "qr.svg")
            QrKtorFormat.PNG -> QrKtorResponse(bytes, ContentType.Image.PNG, "qr.png")
            QrKtorFormat.JPEG -> QrKtorResponse(bytes, ContentType.Image.JPEG, "qr.jpg")
            QrKtorFormat.PDF -> QrKtorResponse(bytes, ContentType.Application.Pdf, "qr.pdf")
        }
    }

    fun generateQr(request: QrGenerateRequest): QrKtorResponse {
        val config = QrRequestMapper.toConfig(request, profiles)
        val format = runCatching { QrKtorFormat.valueOf(request.format.uppercase()) }.getOrDefault(QrKtorFormat.SVG)
        return generateQr(request.data, config, format)
    }
}

data class QrKtorResponse(
    val data: ByteArray,
    val contentType: ContentType,
    val filename: String
)

enum class QrKtorFormat { SVG, PNG, JPEG, PDF }

fun Route.qrGenRoutes(
    path: String = "/qr",
    service: QrGenKtorService = QrGenKtorService()
) {
    route(path) {
        get("/generate") {
            val data = call.parameters["data"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'data' query parameter")

            val request = QrGenerateRequest(
                data = data,
                format = call.parameters["format"] ?: "SVG",
                width = call.parameters["width"]?.toIntOrNull() ?: 512,
                height = call.parameters["height"]?.toIntOrNull() ?: 512,
                margin = call.parameters["margin"]?.toIntOrNull() ?: 16,
                foregroundColor = call.parameters["foregroundColor"] ?: "#000000",
                backgroundColor = call.parameters["backgroundColor"] ?: "#ffffff",
                backgroundCornerRadius = call.parameters["backgroundCornerRadius"]?.toDoubleOrNull() ?: 0.0,
                errorCorrection = call.parameters["errorCorrection"] ?: "QUARTILE",
                moduleType = call.parameters["moduleType"],
                roundSize = call.parameters["roundSize"]?.toBoolean() ?: false,
                moduleScale = call.parameters["moduleScale"]?.toDoubleOrNull() ?: 1.0,
                cornerStyle = call.parameters["cornerStyle"],
                cornerColor = call.parameters["cornerColor"] ?: "#000000",
                cornerLogo = call.parameters["cornerLogo"],
                alignmentPatternShape = call.parameters["alignmentPatternShape"],
                alignmentPatternColor = call.parameters["alignmentPatternColor"],
                alignmentPatternSizeRatio = call.parameters["alignmentPatternSizeRatio"]?.toDoubleOrNull() ?: 0.9,
                animationPreset = call.parameters["animationPreset"]
            )
            call.respondQr(service.generateQr(request))
        }

        post("/generate") {
            val request = call.receiveNullable<QrGenerateRequest>()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing request body")
            call.respondQr(service.generateQr(request))
        }

        get("/health") {
            call.respond(
                mapOf(
                    "status" to "UP",
                    "service" to "QRGen",
                    "framework" to "Ktor",
                    "formats" to QrKtorFormat.entries.map { it.name }
                )
            )
        }
    }
}

private suspend fun ApplicationCall.respondQr(response: QrKtorResponse) {
    this.response.header(HttpHeaders.ContentDisposition, "inline; filename=\"${response.filename}\"")
    respondBytes(response.data, response.contentType)
}
