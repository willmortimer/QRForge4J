package io.github.qrgen.core

import io.nayuki.qrcodegen.QrCode

data class QrGenerateRequest(
    val data: String,
    val format: String = "SVG",
    val width: Int = 512,
    val height: Int = 512,
    val margin: Int = 16,
    val foregroundColor: String = "#000000",
    val backgroundColor: String? = "#ffffff",
    val backgroundCornerRadius: Double = 0.0,
    val errorCorrection: String = "QUARTILE",
    val moduleType: String? = null,
    val roundSize: Boolean = false,
    val moduleScale: Double = 1.0,
    val cornerStyle: String? = null,
    val cornerColor: String = "#000000",
    val cornerLogo: String? = null,
    val alignmentPatternShape: String? = null,
    val alignmentPatternColor: String? = null,
    val alignmentPatternSizeRatio: Double = 0.9,
    val animationPreset: String? = null,
    val animationDurationSeconds: Double = 1.5,
    val profile: String? = null
)

object QrRequestMapper {
    fun toConfig(request: QrGenerateRequest, registry: QrProfileRegistry = QrProfileRegistry()): QrStyleConfig {
        val base = registry.resolve(request.profile) ?: QrStyleConfig()
        val locatorOptions = request.cornerStyle?.let {
            LocatorOptions(enabled = true).withLegacyShape(parseLocatorShape(it), request.cornerColor).let { options ->
                if (request.cornerLogo != null) {
                    options.copy(defaultStyle = options.defaultStyle.copy(logo = LocatorLogoOptions(request.cornerLogo)))
                } else {
                    options
                }
            }
        } ?: base.locators

        val alignmentOptions = request.alignmentPatternShape?.let {
            AlignmentPatternOptions(
                enabled = true,
                shape = parseAlignmentShape(it),
                color = request.alignmentPatternColor,
                sizeRatio = request.alignmentPatternSizeRatio
            )
        } ?: base.alignmentPatterns

        val animationOptions = request.animationPreset?.let {
            AnimationOptions(
                enabled = true,
                preset = parseAnimationPreset(it),
                durationSeconds = request.animationDurationSeconds
            )
        } ?: base.animation

        return base.copy(
            layout = base.layout.copy(
                width = request.width,
                height = request.height,
                margin = request.margin,
                backgroundCornerRadius = request.backgroundCornerRadius
            ),
            colors = base.colors.copy(
                foreground = request.foregroundColor,
                background = request.backgroundColor
            ),
            modules = base.modules.copy(
                type = request.moduleType?.uppercase()?.let { runCatching { DotType.valueOf(it) }.getOrNull() } ?: base.modules.type,
                roundSize = request.roundSize,
                sizeScale = request.moduleScale
            ),
            locators = locatorOptions,
            alignmentPatterns = alignmentOptions,
            animation = animationOptions,
            qrOptions = base.qrOptions.copy(ecc = parseErrorCorrection(request.errorCorrection))
        )
    }

    fun parseErrorCorrection(value: String?): QrCode.Ecc =
        when (value?.uppercase()) {
            "LOW", "L" -> QrCode.Ecc.LOW
            "MEDIUM", "M" -> QrCode.Ecc.MEDIUM
            "HIGH", "H" -> QrCode.Ecc.HIGH
            else -> QrCode.Ecc.QUARTILE
        }

    fun parseLocatorShape(style: String): LocatorShape =
        when (style.uppercase()) {
            "SQUARE" -> LocatorShape.Square
            "CIRCLE" -> LocatorShape.Circle
            "ROUNDED" -> LocatorShape.Rounded()
            "CLASSY" -> LocatorShape.Classy
            else -> LocatorShape.Square
        }

    fun parseAlignmentShape(shape: String): AlignmentPatternShape =
        when (shape.uppercase()) {
            "SQUARE" -> AlignmentPatternShape.SQUARE
            "DIAMOND" -> AlignmentPatternShape.DIAMOND
            "STAR" -> AlignmentPatternShape.STAR
            else -> AlignmentPatternShape.CIRCLE
        }

    fun parseAnimationPreset(value: String): AnimationPreset =
        when (value.uppercase()) {
            "PULSE" -> AnimationPreset.PULSE
            "DRAW_IN", "DRAW-IN" -> AnimationPreset.DRAW_IN
            else -> AnimationPreset.FADE
        }
}
