package qrcli

import io.nayuki.qrcodegen.QrCode
import java.io.File

fun main(args: Array<String>) {
    val cfg = parseArgs(args)

    val inputBytes = cfg.inputFile
        ?.let { File(it).readBytes() }
        ?: System.`in`.readBytes()

    val payload = inputBytes.decodeToString()
    val svg = when (cfg.encoding.lowercase()) {
        "base64"  -> renderSvgBase64(payload, cfg.toRenderOpts())
        "latin1"  -> renderSvgLatin1(payload, cfg.toRenderOpts())
        else      -> error("Unsupported encoding: ${cfg.encoding}")
    }

    if (cfg.outputFile != null) File(cfg.outputFile).writeText(svg) else print(svg)
}

private data class CliCfg(
    val inputFile: String? = null,
    val outputFile: String? = null,
    val encoding: String = "base64",
    val width: Int = 512,
    val height: Int = 512,
    val margin: Int = 16,
    val circle: Boolean = false,
    val dots: String = "circle",
    val rounded: Boolean = false,
    val extraRounded: Boolean = false,
    val classyRounded: Boolean = false,
    val dotRadiusFactor: Double = 0.5,
    val fg: String = "#000",
    val bg: String? = "#fff",
    val hole: Double? = null,
    val ec: String = "q",
    val logo: String? = null,
    val logoSize: Double = 0.2,
    val cornerStyle: String? = null,
    val cornerColor: String = "#000",
    val cornerSize: Double = 7.0,
    // Advanced features
    val moduleOutline: Boolean = false,
    val moduleOutlineColor: String = "#111111",
    val moduleOutlineWidth: Double = 0.5,
    val quietZone: Boolean = false,
    val quietZoneColor: String = "#444444",
    val quietZoneWidth: Double = 1.0,
    val quietZoneDash: String = "4 4",
    val dropShadow: Boolean = false,
    val shadowBlur: Double = 1.0,
    val shadowOpacity: Double = 0.2,
    val shadowX: Double = 0.0,
    val shadowY: Double = 0.0,
    val bgPattern: String? = null,
    val bgPatternColor: String = "#f0f0f0",
    val bgPatternOpacity: Double = 0.02,
    val bgPatternSize: Double = 4.0,
    val gradientMask: String? = null,
    val gradientCenter: String? = null,
    val gradientEdge: String? = null,
    val microText: String? = null,
    val microTextSize: Double = 8.0,
    val microTextColor: String = "#666666",
    val microTextPath: String = "circular"
) {
    fun toRenderOpts(): RenderOpts {
        val ecc = when (ec.lowercase()) {
            "l" -> QrCode.Ecc.LOW; "m" -> QrCode.Ecc.MEDIUM; "h" -> QrCode.Ecc.HIGH
            else -> QrCode.Ecc.QUARTILE
        }
        val dt = when {
            extraRounded -> DotType.EXTRA_ROUNDED
            classyRounded -> DotType.CLASSY_ROUNDED
            dots.lowercase() == "square" -> DotType.SQUARE
            dots.lowercase() == "classy" -> DotType.CLASSY
            dots.lowercase() == "rounded" -> DotType.ROUNDED
            else -> DotType.CIRCLE
        }
        val locOpts = cornerStyle?.lowercase()?.let { style ->
            val shape = when (style) {
                "square"  -> LocatorShape.Square
                "circle"  -> LocatorShape.Circle
                "rounded" -> LocatorShape.Rounded()
                "classy"  -> LocatorShape.Classy
                else      -> null
            }
            shape?.let { LocatorOpts(it, cornerColor, cornerSize) }
        }
        return RenderOpts(
            width                 = width,
            height                = height,
            margin                = margin,
            circleShape           = circle,
            dotsType              = dt,
            rounded               = rounded && dt==DotType.SQUARE,
            extraRounded          = extraRounded,
            classyRounded         = classyRounded,
            dotRadiusFactor       = dotRadiusFactor,
            fgColor               = fg,
            bgColor               = bg,
            logoHoleRadiusPx      = hole,
            centerImageHref       = logo,
            centerImageSizeRatio  = logoSize,
            locatorOpts           = locOpts,
            ecc                   = ecc,
            moduleOutline         = if (moduleOutline) ModuleOutline(true, moduleOutlineColor, moduleOutlineWidth) else null,
            quietZoneAccent       = if (quietZone) QuietZoneAccent(true, quietZoneColor, quietZoneWidth, quietZoneDash) else null,
            dropShadow            = if (dropShadow) DropShadow(true, shadowBlur, shadowOpacity, shadowX, shadowY) else null,
            backgroundPattern     = bgPattern?.let { 
                val patternType = when (it.lowercase()) {
                    "dots" -> PatternType.DOTS
                    "grid" -> PatternType.GRID
                    "diagonal" -> PatternType.DIAGONAL_LINES
                    "hexagon" -> PatternType.HEXAGON
                    else -> PatternType.DOTS
                }
                BackgroundPattern(true, patternType, bgPatternColor, bgPatternOpacity, bgPatternSize) 
            },
            gradientMasking       = gradientMask?.let {
                val maskType = when (it.lowercase()) {
                    "concentric" -> MaskingType.CONCENTRIC
                    "radial" -> MaskingType.RADIAL
                    "linear" -> MaskingType.LINEAR
                    else -> MaskingType.CONCENTRIC
                }
                GradientMasking(true, maskType, gradientCenter, gradientEdge)
            },
            microTypography       = microText?.let {
                val pathType = when (microTextPath.lowercase()) {
                    "circular" -> TypographyPath.CIRCULAR
                    "top" -> TypographyPath.LINEAR_TOP
                    "bottom" -> TypographyPath.LINEAR_BOTTOM
                    else -> TypographyPath.CIRCULAR
                }
                MicroTypography(true, it, microTextSize, microTextColor, pathType)
            }
        )
    }
}

private fun parseArgs(a: Array<String>): CliCfg {
    var cfg = CliCfg()
    var i = 0
    fun nxt() = a[++i]
    while (i < a.size) {
        when (a[i]) {
            "-i","--input"        -> cfg = cfg.copy(inputFile      = nxt())
            "-o","--output"       -> cfg = cfg.copy(outputFile     = nxt())
            "--enc"               -> cfg = cfg.copy(encoding       = nxt())
            "--width"             -> cfg = cfg.copy(width          = nxt().toInt())
            "--height"            -> cfg = cfg.copy(height         = nxt().toInt())
            "--margin"            -> cfg = cfg.copy(margin         = nxt().toInt())
            "--circle"            -> cfg = cfg.copy(circle         = true)
            "--dots"              -> cfg = cfg.copy(dots           = nxt())
            "--rounded"           -> cfg = cfg.copy(rounded        = true)
            "--dot-radius"        -> cfg = cfg.copy(dotRadiusFactor= nxt().toDouble())
            "--fg"                -> cfg = cfg.copy(fg             = nxt())
            "--bg"                -> cfg = cfg.copy(bg             = nxt().ifBlank { null })
            "--hole"              -> cfg = cfg.copy(hole           = nxt().toDoubleOrNull())
            "--ec"                -> cfg = cfg.copy(ec             = nxt())
            "--logo"              -> cfg = cfg.copy(logo           = nxt())
            "--logo-size"         -> cfg = cfg.copy(logoSize       = nxt().toDouble())
            "--corner-style"      -> cfg = cfg.copy(cornerStyle    = nxt())
            "--corner-color"      -> cfg = cfg.copy(cornerColor    = nxt())
            "--corner-size"       -> cfg = cfg.copy(cornerSize     = nxt().toDouble())
            // Advanced styling options
            "--extra-rounded"     -> cfg = cfg.copy(extraRounded   = true)
            "--classy-rounded"    -> cfg = cfg.copy(classyRounded  = true)
            "--module-outline"    -> cfg = cfg.copy(moduleOutline  = true)
            "--outline-color"     -> cfg = cfg.copy(moduleOutlineColor = nxt())
            "--outline-width"     -> cfg = cfg.copy(moduleOutlineWidth = nxt().toDouble())
            "--quiet-zone"        -> cfg = cfg.copy(quietZone      = true)
            "--quiet-color"       -> cfg = cfg.copy(quietZoneColor = nxt())
            "--quiet-width"       -> cfg = cfg.copy(quietZoneWidth = nxt().toDouble())
            "--quiet-dash"        -> cfg = cfg.copy(quietZoneDash  = nxt())
            "--drop-shadow"       -> cfg = cfg.copy(dropShadow     = true)
            "--shadow-blur"       -> cfg = cfg.copy(shadowBlur     = nxt().toDouble())
            "--shadow-opacity"    -> cfg = cfg.copy(shadowOpacity  = nxt().toDouble())
            "--shadow-x"          -> cfg = cfg.copy(shadowX        = nxt().toDouble())
            "--shadow-y"          -> cfg = cfg.copy(shadowY        = nxt().toDouble())
            "--bg-pattern"        -> cfg = cfg.copy(bgPattern      = nxt())
            "--pattern-color"     -> cfg = cfg.copy(bgPatternColor = nxt())
            "--pattern-opacity"   -> cfg = cfg.copy(bgPatternOpacity = nxt().toDouble())
            "--pattern-size"      -> cfg = cfg.copy(bgPatternSize  = nxt().toDouble())
            "--gradient-mask"     -> cfg = cfg.copy(gradientMask   = nxt())
            "--gradient-center"   -> cfg = cfg.copy(gradientCenter = nxt())
            "--gradient-edge"     -> cfg = cfg.copy(gradientEdge   = nxt())
            "--micro-text"        -> cfg = cfg.copy(microText      = nxt())
            "--micro-size"        -> cfg = cfg.copy(microTextSize  = nxt().toDouble())
            "--micro-color"       -> cfg = cfg.copy(microTextColor = nxt())
            "--micro-path"        -> cfg = cfg.copy(microTextPath  = nxt())
            "-h","--help"         -> { printHelp(); return cfg }
            else                  -> error("Unknown argument: ${a[i]}")
        }
        i++
    }
    return cfg
}

private fun printHelp() {
    println("""
Usage:
  echo "<base64|latin1 payload>" | qrcli [options]
  qrcli --input data.txt --enc latin1 --logo logo.png [options]

Basic Options:
  -i, --input              Input file (default stdin)
  -o, --output             Output SVG file (default stdout)
  --enc <base64|latin1>    Encoding of payload (default base64)
  --width <px>             SVG canvas width (default 512)
  --height <px>            SVG canvas height (default 512)
  --margin <px>            Margin around QR (default 16)
  --circle                 Crop QR into a circle
  --dots <circle|square|classy|rounded>
                           Dot style (default circle)
  --rounded                Rounded squares (only with square dots)
  --dot-radius <0.0–0.5>   Radius factor (default 0.5)
  --fg <color>             Foreground color (default #000)
  --bg <color|null>        Background color (default #fff)
  --hole <px>              Carve out a logo hole (px)
  --ec <l|m|q|h>           Error correction level (default q)

Logo & Corners:
  --logo <url>             URL or data-URI for center image
  --logo-size <0.0–1.0>    Center image size ratio (default 0.2)
  --corner-style <style>   Corner locator: square|circle|rounded|classy
  --corner-color <color>   Locator color (default #000)
  --corner-size <modules>  Locator box size in modules (default 7.0)

Advanced Module Styles:
  --extra-rounded          Extra large corner radius for modules
  --classy-rounded         Combination of ring and rounded styles
  --module-outline         Add subtle outline to modules
  --outline-color <color>  Module outline color (default #111111)
  --outline-width <px>     Module outline width (default 0.5)

Visual Enhancements:
  --quiet-zone             Add accent border around quiet zone
  --quiet-color <color>    Quiet zone border color (default #444444)
  --quiet-width <px>       Quiet zone border width (default 1.0)
  --quiet-dash <pattern>   Dash pattern for quiet zone (default "4 4")
  --drop-shadow            Add subtle drop shadow effect
  --shadow-blur <px>       Shadow blur radius (default 1.0)
  --shadow-opacity <0-1>   Shadow opacity (default 0.2)
  --shadow-x <px>          Shadow X offset (default 0.0)
  --shadow-y <px>          Shadow Y offset (default 0.0)

Background Patterns:
  --bg-pattern <type>      Background pattern: dots|grid|diagonal|hexagon
  --pattern-color <color>  Pattern color (default #f0f0f0)
  --pattern-opacity <0-1>  Pattern opacity (default 0.02)
  --pattern-size <px>      Pattern size (default 4.0)

Gradient Effects:
  --gradient-mask <type>   Gradient masking: concentric|radial|linear
  --gradient-center <color> Center color for gradient masking
  --gradient-edge <color>  Edge color for gradient masking

Typography:
  --micro-text <text>      Micro text around border
  --micro-size <px>        Micro text font size (default 8.0)
  --micro-color <color>    Micro text color (default #666666)
  --micro-path <path>      Text path: circular|top|bottom (default circular)

  -h, --help               Show this help

Examples:
  # Extra-rounded blue QR with drop shadow
  echo "Hello!" | qrcli --enc latin1 --extra-rounded --fg #0066cc --drop-shadow

  # QR with background pattern and quiet zone accent
  echo "Styled" | qrcli --enc latin1 --bg-pattern dots --quiet-zone

  # Gradient-masked QR with micro typography
  echo "Advanced" | qrcli --enc latin1 --gradient-mask concentric --gradient-center #000 --gradient-edge #666 --micro-text "Secure • Verified"
""".trimIndent())
}
