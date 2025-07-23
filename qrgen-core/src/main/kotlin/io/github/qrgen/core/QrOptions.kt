package io.github.qrgen.core

import io.nayuki.qrcodegen.QrCode.Ecc

/** Core QR generation options **/
data class QrOptions(
    val ecc: Ecc = Ecc.QUARTILE,
    val mask: Int = -1,
    val minVersion: Int = 1,
    val maxVersion: Int = 40
)

/** Layout and sizing options **/
data class LayoutOptions(
    val width: Int = 512,
    val height: Int = 512,
    val margin: Int = 16,
    val circleShape: Boolean = false
)

/** Module styling options **/
data class ModuleOptions(
    val type: DotType = DotType.CIRCLE,
    val radiusFactor: Double = 0.5,
    val rounded: Boolean = false,
    val extraRounded: Boolean = false,
    val classyRounded: Boolean = false
)

enum class DotType { 
    CIRCLE, SQUARE, CLASSY, ROUNDED, EXTRA_ROUNDED, CLASSY_ROUNDED 
}

/** Color and visual styling **/
data class ColorOptions(
    val foreground: String = "#000000",
    val background: String? = "#ffffff"
)

/** Logo and center image options **/
data class LogoOptions(
    val href: String? = null,
    val sizeRatio: Double = 0.2,
    val holeRadiusPx: Double? = null
)

/** Corner locator (finder pattern) styling **/
data class LocatorOptions(
    val shape: LocatorShape? = null,
    val color: String = "#000000",
    val sizeRatio: Double = 7.0
)

sealed class LocatorShape {
    object Square : LocatorShape()
    object Circle : LocatorShape()
    data class Rounded(val radiusFactor: Double = 0.35) : LocatorShape()
    object Classy : LocatorShape()
}

/** Gradient specifications **/
data class GradientOptions(
    val type: GradientType? = null,
    val stops: List<ColorStop> = emptyList(),
    val rotationRad: Double = 0.0
)

enum class GradientType { LINEAR, RADIAL }

data class ColorStop(val offset: Double, val color: String)

/** Border specifications **/
data class BorderOptions(
    val thickness: Double = 0.0,
    val color: String = "#000000",
    val round: Double = 0.0,
    val inner: BorderOptions? = null,
    val outer: BorderOptions? = null
)

/** Advanced visual effects **/
data class AdvancedOptions(
    val moduleOutline: ModuleOutline? = null,
    val quietZoneAccent: QuietZoneAccent? = null,
    val dropShadow: DropShadow? = null,
    val backgroundPattern: BackgroundPattern? = null,
    val gradientMasking: GradientMasking? = null,
    val microTypography: MicroTypography? = null
)

data class ModuleOutline(
    val enabled: Boolean = false,
    val color: String = "#111111",
    val width: Double = 0.5
)

data class QuietZoneAccent(
    val enabled: Boolean = false,
    val color: String = "#444444",
    val width: Double = 1.0,
    val dashArray: String = "4 4"
)

data class DropShadow(
    val enabled: Boolean = false,
    val blur: Double = 1.0,
    val opacity: Double = 0.2,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0
)

data class BackgroundPattern(
    val enabled: Boolean = false,
    val type: PatternType = PatternType.DOTS,
    val color: String = "#f0f0f0",
    val opacity: Double = 0.02,
    val size: Double = 4.0
)

enum class PatternType { DOTS, GRID, DIAGONAL_LINES, HEXAGON }

data class GradientMasking(
    val enabled: Boolean = false,
    val type: MaskingType = MaskingType.CONCENTRIC,
    val centerColor: String? = null,
    val edgeColor: String? = null
)

enum class MaskingType { CONCENTRIC, RADIAL, LINEAR }

data class MicroTypography(
    val enabled: Boolean = false,
    val text: String = "",
    val fontSize: Double = 8.0,
    val color: String = "#666666",
    val path: TypographyPath = TypographyPath.CIRCULAR
)

enum class TypographyPath { CIRCULAR, LINEAR_TOP, LINEAR_BOTTOM }

/** Complete QR styling configuration **/
data class QrStyleConfig(
    val qrOptions: QrOptions = QrOptions(),
    val layout: LayoutOptions = LayoutOptions(),
    val modules: ModuleOptions = ModuleOptions(),
    val colors: ColorOptions = ColorOptions(),
    val logo: LogoOptions = LogoOptions(),
    val locators: LocatorOptions = LocatorOptions(),
    val gradient: GradientOptions = GradientOptions(),
    val border: BorderOptions = BorderOptions(),
    val advanced: AdvancedOptions = AdvancedOptions()
) 