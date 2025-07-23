package io.github.qrgen.dsl

import io.github.qrgen.core.*
import io.github.qrgen.svg.DefaultSvgRenderer
import io.nayuki.qrcodegen.QrCode.Ecc

/** Main entry point for the QR code DSL **/
object QRCode {
    
    fun ofSquares(): QrCodeBuilder = QrCodeBuilder().dotStyle { type = DotType.SQUARE }
    fun ofCircles(): QrCodeBuilder = QrCodeBuilder().dotStyle { type = DotType.CIRCLE }
    fun ofRoundedSquares(): QrCodeBuilder = QrCodeBuilder().dotStyle { type = DotType.ROUNDED }
    fun ofExtraRounded(): QrCodeBuilder = QrCodeBuilder().dotStyle { type = DotType.EXTRA_ROUNDED }
    fun ofClassyRings(): QrCodeBuilder = QrCodeBuilder().dotStyle { type = DotType.CLASSY }
    fun ofClassyRounded(): QrCodeBuilder = QrCodeBuilder().dotStyle { type = DotType.CLASSY_ROUNDED }
    
    fun custom(): QrCodeBuilder = QrCodeBuilder()
}

/** Main QR code builder with fluent DSL **/
class QrCodeBuilder {
    private var config = QrStyleConfig()
    
    // Layout configuration
    fun width(pixels: Int) = apply { config = config.copy(layout = config.layout.copy(width = pixels)) }
    fun height(pixels: Int) = apply { config = config.copy(layout = config.layout.copy(height = pixels)) }
    fun size(pixels: Int) = apply { 
        config = config.copy(layout = config.layout.copy(width = pixels, height = pixels))
    }
    fun margin(pixels: Int) = apply { config = config.copy(layout = config.layout.copy(margin = pixels)) }
    fun circleShape(enabled: Boolean = true) = apply { 
        config = config.copy(layout = config.layout.copy(circleShape = enabled))
    }
    
    // QR options
    fun errorCorrection(level: Ecc) = apply {
        config = config.copy(qrOptions = config.qrOptions.copy(ecc = level))
    }
    fun mask(pattern: Int) = apply {
        config = config.copy(qrOptions = config.qrOptions.copy(mask = pattern))
    }
    fun versionRange(min: Int, max: Int) = apply {
        config = config.copy(qrOptions = config.qrOptions.copy(minVersion = min, maxVersion = max))
    }
    
    // Colors
    fun withColor(color: String) = apply {
        config = config.copy(colors = config.colors.copy(foreground = color))
    }
    fun withBackground(color: String?) = apply {
        config = config.copy(colors = config.colors.copy(background = color))
    }
    fun colors(block: ColorOptionsBuilder.() -> Unit) = apply {
        val builder = ColorOptionsBuilder(config.colors)
        builder.block()
        config = config.copy(colors = builder.build())
    }
    
    // Module styling
    fun dotStyle(block: ModuleOptionsBuilder.() -> Unit) = apply {
        val builder = ModuleOptionsBuilder(config.modules)
        builder.block()
        config = config.copy(modules = builder.build())
    }
    
    // Logo and center image
    fun centerImage(href: String, sizeRatio: Double = 0.2) = apply {
        config = config.copy(logo = config.logo.copy(href = href, sizeRatio = sizeRatio))
    }
    fun logoHole(radiusPx: Double) = apply {
        config = config.copy(logo = config.logo.copy(holeRadiusPx = radiusPx))
    }
    fun logo(block: LogoOptionsBuilder.() -> Unit) = apply {
        val builder = LogoOptionsBuilder(config.logo)
        builder.block()
        config = config.copy(logo = builder.build())
    }
    
    // Corner locators
    fun cornerLocator(block: LocatorOptionsBuilder.() -> Unit) = apply {
        val builder = LocatorOptionsBuilder(config.locators)
        builder.block()
        config = config.copy(locators = builder.build())
    }
    
    // Gradients
    fun linearGradient(block: GradientOptionsBuilder.() -> Unit) = apply {
        val builder = GradientOptionsBuilder(config.gradient.copy(type = GradientType.LINEAR))
        builder.block()
        config = config.copy(gradient = builder.build())
    }
    fun radialGradient(block: GradientOptionsBuilder.() -> Unit) = apply {
        val builder = GradientOptionsBuilder(config.gradient.copy(type = GradientType.RADIAL))
        builder.block()
        config = config.copy(gradient = builder.build())
    }
    
    // Borders
    fun border(block: BorderOptionsBuilder.() -> Unit) = apply {
        val builder = BorderOptionsBuilder(config.border)
        builder.block()
        config = config.copy(border = builder.build())
    }
    
    // Advanced effects
    fun moduleOutline(color: String = "#111111", width: Double = 0.5) = apply {
        config = config.copy(advanced = config.advanced.copy(
            moduleOutline = ModuleOutline(true, color, width)
        ))
    }
    
    fun quietZoneAccent(color: String = "#444444", width: Double = 1.0, dashArray: String = "4 4") = apply {
        config = config.copy(advanced = config.advanced.copy(
            quietZoneAccent = QuietZoneAccent(true, color, width, dashArray)
        ))
    }
    
    fun dropShadow(blur: Double = 1.0, opacity: Double = 0.2, offsetX: Double = 0.0, offsetY: Double = 0.0) = apply {
        config = config.copy(advanced = config.advanced.copy(
            dropShadow = DropShadow(true, blur, opacity, offsetX, offsetY)
        ))
    }
    
    fun backgroundPattern(block: BackgroundPatternBuilder.() -> Unit) = apply {
        val builder = BackgroundPatternBuilder()
        builder.block()
        config = config.copy(advanced = config.advanced.copy(backgroundPattern = builder.build()))
    }
    
    fun gradientMasking(block: GradientMaskingBuilder.() -> Unit) = apply {
        val builder = GradientMaskingBuilder()
        builder.block()
        config = config.copy(advanced = config.advanced.copy(gradientMasking = builder.build()))
    }
    
    fun microTypography(text: String, block: MicroTypographyBuilder.() -> Unit = {}) = apply {
        val builder = MicroTypographyBuilder().apply { this.text = text }
        builder.block()
        config = config.copy(advanced = config.advanced.copy(microTypography = builder.build()))
    }
    
    fun advanced(block: AdvancedOptionsBuilder.() -> Unit) = apply {
        val builder = AdvancedOptionsBuilder(config.advanced)
        builder.block()
        config = config.copy(advanced = builder.build())
    }
    
    // Build methods
    fun build(data: String): QrResult {
        val generator = DefaultQrGenerator()
        return generator.generateFromText(data, config)
    }
    
    fun buildFromBytes(data: ByteArray): QrResult {
        val generator = DefaultQrGenerator()
        return generator.generateFromBytes(data, config)
    }
    
    fun buildSvg(data: String): String {
        val qrResult = build(data)
        val renderer = DefaultSvgRenderer()
        return renderer.render(qrResult)
    }
    
    fun buildSvgFromBytes(data: ByteArray): String {
        val qrResult = buildFromBytes(data)
        val renderer = DefaultSvgRenderer()
        return renderer.render(qrResult)
    }
    
    // Async versions (for future use)
    suspend fun buildSvgAsync(data: String): String = buildSvg(data)
    suspend fun buildSvgFromBytesAsync(data: ByteArray): String = buildSvgFromBytes(data)
}

/** Color options builder **/
class ColorOptionsBuilder(private var options: ColorOptions) {
    var foreground: String
        get() = options.foreground
        set(value) { options = options.copy(foreground = value) }
    
    var background: String?
        get() = options.background
        set(value) { options = options.copy(background = value) }
    
    fun transparent() { background = null }
    
    internal fun build() = options
}

/** Module options builder **/
class ModuleOptionsBuilder(private var options: ModuleOptions) {
    var type: DotType
        get() = options.type
        set(value) { options = options.copy(type = value) }
    
    var radiusFactor: Double
        get() = options.radiusFactor
        set(value) { options = options.copy(radiusFactor = value) }
    
    var rounded: Boolean
        get() = options.rounded
        set(value) { options = options.copy(rounded = value) }
    
    var extraRounded: Boolean
        get() = options.extraRounded
        set(value) { options = options.copy(extraRounded = value) }
    
    var classyRounded: Boolean
        get() = options.classyRounded
        set(value) { options = options.copy(classyRounded = value) }
    
    internal fun build() = options
}

/** Logo options builder **/
class LogoOptionsBuilder(private var options: LogoOptions) {
    var href: String?
        get() = options.href
        set(value) { options = options.copy(href = value) }
    
    var sizeRatio: Double
        get() = options.sizeRatio
        set(value) { options = options.copy(sizeRatio = value) }
    
    var holeRadiusPx: Double?
        get() = options.holeRadiusPx
        set(value) { options = options.copy(holeRadiusPx = value) }
    
    fun image(url: String, size: Double = 0.2) {
        href = url
        sizeRatio = size
    }
    
    fun hole(radius: Double) {
        holeRadiusPx = radius
    }
    
    internal fun build() = options
}

/** Locator options builder **/
class LocatorOptionsBuilder(private var options: LocatorOptions) {
    var color: String
        get() = options.color
        set(value) { options = options.copy(color = value) }
    
    var sizeRatio: Double
        get() = options.sizeRatio
        set(value) { options = options.copy(sizeRatio = value) }
    
    fun square() { options = options.copy(shape = LocatorShape.Square) }
    fun circle() { options = options.copy(shape = LocatorShape.Circle) }
    fun rounded(radiusFactor: Double = 0.35) { 
        options = options.copy(shape = LocatorShape.Rounded(radiusFactor)) 
    }
    fun classy() { options = options.copy(shape = LocatorShape.Classy) }
    
    internal fun build() = options
}

/** Gradient options builder **/
class GradientOptionsBuilder(private var options: GradientOptions) {
    var rotationRad: Double
        get() = options.rotationRad
        set(value) { options = options.copy(rotationRad = value) }
    
    fun rotation(degrees: Double) {
        rotationRad = Math.toRadians(degrees)
    }
    
    fun colorStop(offset: Double, color: String) {
        options = options.copy(stops = options.stops + ColorStop(offset, color))
    }
    
    fun stops(vararg stops: Pair<Double, String>) {
        options = options.copy(stops = stops.map { ColorStop(it.first, it.second) })
    }
    
    internal fun build() = options
}

/** Border options builder **/
class BorderOptionsBuilder(private var options: BorderOptions) {
    var thickness: Double
        get() = options.thickness
        set(value) { options = options.copy(thickness = value) }
    
    var color: String
        get() = options.color
        set(value) { options = options.copy(color = value) }
    
    var round: Double
        get() = options.round
        set(value) { options = options.copy(round = value) }
    
    fun inner(block: BorderOptionsBuilder.() -> Unit) {
        val builder = BorderOptionsBuilder(BorderOptions())
        builder.block()
        options = options.copy(inner = builder.build())
    }
    
    fun outer(block: BorderOptionsBuilder.() -> Unit) {
        val builder = BorderOptionsBuilder(BorderOptions())
        builder.block()
        options = options.copy(outer = builder.build())
    }
    
    internal fun build() = options
}

/** Background pattern builder **/
class BackgroundPatternBuilder {
    private var pattern = BackgroundPattern(enabled = true)
    
    var type: PatternType
        get() = pattern.type
        set(value) { pattern = pattern.copy(type = value) }
    
    var color: String
        get() = pattern.color
        set(value) { pattern = pattern.copy(color = value) }
    
    var opacity: Double
        get() = pattern.opacity
        set(value) { pattern = pattern.copy(opacity = value) }
    
    var size: Double
        get() = pattern.size
        set(value) { pattern = pattern.copy(size = value) }
    
    fun dots() { type = PatternType.DOTS }
    fun grid() { type = PatternType.GRID }
    fun diagonal() { type = PatternType.DIAGONAL_LINES }
    fun hexagon() { type = PatternType.HEXAGON }
    
    internal fun build() = pattern
}

/** Gradient masking builder **/
class GradientMaskingBuilder {
    private var masking = GradientMasking(enabled = true)
    
    var type: MaskingType
        get() = masking.type
        set(value) { masking = masking.copy(type = value) }
    
    var centerColor: String?
        get() = masking.centerColor
        set(value) { masking = masking.copy(centerColor = value) }
    
    var edgeColor: String?
        get() = masking.edgeColor
        set(value) { masking = masking.copy(edgeColor = value) }
    
    fun concentric(center: String, edge: String) {
        type = MaskingType.CONCENTRIC
        centerColor = center
        edgeColor = edge
    }
    
    fun radial(center: String, edge: String) {
        type = MaskingType.RADIAL
        centerColor = center
        edgeColor = edge
    }
    
    fun linear(start: String, end: String) {
        type = MaskingType.LINEAR
        centerColor = start
        edgeColor = end
    }
    
    internal fun build() = masking
}

/** Micro typography builder **/
class MicroTypographyBuilder {
    private var typography = MicroTypography(enabled = true)
    
    var text: String
        get() = typography.text
        set(value) { typography = typography.copy(text = value) }
    
    var fontSize: Double
        get() = typography.fontSize
        set(value) { typography = typography.copy(fontSize = value) }
    
    var color: String
        get() = typography.color
        set(value) { typography = typography.copy(color = value) }
    
    var path: TypographyPath
        get() = typography.path
        set(value) { typography = typography.copy(path = value) }
    
    fun circular() { path = TypographyPath.CIRCULAR }
    fun top() { path = TypographyPath.LINEAR_TOP }
    fun bottom() { path = TypographyPath.LINEAR_BOTTOM }
    
    internal fun build() = typography
}

/** Advanced options builder **/
class AdvancedOptionsBuilder(private var options: AdvancedOptions) {
    
    fun moduleOutline(color: String = "#111111", width: Double = 0.5) {
        options = options.copy(moduleOutline = ModuleOutline(true, color, width))
    }
    
    fun quietZoneAccent(color: String = "#444444", width: Double = 1.0, dashArray: String = "4 4") {
        options = options.copy(quietZoneAccent = QuietZoneAccent(true, color, width, dashArray))
    }
    
    fun dropShadow(blur: Double = 1.0, opacity: Double = 0.2, offsetX: Double = 0.0, offsetY: Double = 0.0) {
        options = options.copy(dropShadow = DropShadow(true, blur, opacity, offsetX, offsetY))
    }
    
    fun backgroundPattern(block: BackgroundPatternBuilder.() -> Unit) {
        val builder = BackgroundPatternBuilder()
        builder.block()
        options = options.copy(backgroundPattern = builder.build())
    }
    
    fun gradientMasking(block: GradientMaskingBuilder.() -> Unit) {
        val builder = GradientMaskingBuilder()
        builder.block()
        options = options.copy(gradientMasking = builder.build())
    }
    
    fun microTypography(text: String, block: MicroTypographyBuilder.() -> Unit = {}) {
        val builder = MicroTypographyBuilder().apply { this.text = text }
        builder.block()
        options = options.copy(microTypography = builder.build())
    }
    
    internal fun build() = options
}

/** Convenience extension functions **/
fun String.toQrCode(): QrCodeBuilder = QRCode.custom()

fun String.toQrSvg(block: QrCodeBuilder.() -> Unit = {}): String {
    val builder = QRCode.custom()
    builder.block()
    return builder.buildSvg(this)
}

/** Color constants for convenience **/
object Colors {
    const val BLACK = "#000000"
    const val WHITE = "#ffffff"
    const val DEEP_SKY_BLUE = "#00bfff"
    const val FOREST_GREEN = "#228b22"
    const val CRIMSON = "#dc143c"
    const val GOLD = "#ffd700"
    const val DARK_VIOLET = "#9400d3"
    const val ORANGE_RED = "#ff4500"
    const val STEEL_BLUE = "#4682b4"
    const val DARK_SLATE_GRAY = "#2f4f4f"
} 