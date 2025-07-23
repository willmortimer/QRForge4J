package io.github.qrgen.cli

import io.github.qrgen.core.*
import io.github.qrgen.dsl.*
import io.nayuki.qrcodegen.QrCode.Ecc
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        val config = parseArgs(args)
        val data = readInput(config.inputFile, config.encoding)
        val svg = generateQr(data, config)
        writeOutput(svg, config.outputFile)
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}

private data class CliConfig(
    val inputFile: String? = null,
    val outputFile: String? = null,
    val encoding: String = "utf8",
    val width: Int = 512,
    val height: Int = 512,
    val margin: Int = 16,
    val dots: String = "circle",
    val fg: String = "#000000",
    val bg: String? = "#ffffff",
    val ec: String = "q",
    val logo: String? = null,
    val logoSize: Double = 0.2,
    val cornerStyle: String? = null,
    val cornerColor: String = "#000000",
    // Advanced styling
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
)

private fun parseArgs(args: Array<String>): CliConfig {
    var config = CliConfig()
    var i = 0
    
    fun nxt(): String {
        if (++i >= args.size) error("Missing argument after ${args[i-1]}")
        return args[i]
    }
    
    while (i < args.size) {
        when (args[i]) {
            "-i", "--input" -> config = config.copy(inputFile = nxt())
            "-o", "--output" -> config = config.copy(outputFile = nxt())
            "--enc" -> config = config.copy(encoding = nxt())
            "--width" -> config = config.copy(width = nxt().toInt())
            "--height" -> config = config.copy(height = nxt().toInt())
            "--size" -> {
                val size = nxt().toInt()
                config = config.copy(width = size, height = size)
            }
            "--margin" -> config = config.copy(margin = nxt().toInt())
            "--dots" -> config = config.copy(dots = nxt())
            "--fg" -> config = config.copy(fg = nxt())
            "--bg" -> {
                val bg = nxt()
                config = config.copy(bg = if (bg == "null") null else bg)
            }
            "--ec" -> config = config.copy(ec = nxt())
            "--logo" -> config = config.copy(logo = nxt())
            "--logo-size" -> config = config.copy(logoSize = nxt().toDouble())
            "--corner-style" -> config = config.copy(cornerStyle = nxt())
            "--corner-color" -> config = config.copy(cornerColor = nxt())
            // Advanced features
            "--module-outline" -> config = config.copy(moduleOutline = true)
            "--outline-color" -> config = config.copy(moduleOutlineColor = nxt())
            "--outline-width" -> config = config.copy(moduleOutlineWidth = nxt().toDouble())
            "--quiet-zone" -> config = config.copy(quietZone = true)
            "--quiet-color" -> config = config.copy(quietZoneColor = nxt())
            "--quiet-width" -> config = config.copy(quietZoneWidth = nxt().toDouble())
            "--quiet-dash" -> config = config.copy(quietZoneDash = nxt())
            "--drop-shadow" -> config = config.copy(dropShadow = true)
            "--shadow-blur" -> config = config.copy(shadowBlur = nxt().toDouble())
            "--shadow-opacity" -> config = config.copy(shadowOpacity = nxt().toDouble())
            "--shadow-x" -> config = config.copy(shadowX = nxt().toDouble())
            "--shadow-y" -> config = config.copy(shadowY = nxt().toDouble())
            "--bg-pattern" -> config = config.copy(bgPattern = nxt())
            "--pattern-color" -> config = config.copy(bgPatternColor = nxt())
            "--pattern-opacity" -> config = config.copy(bgPatternOpacity = nxt().toDouble())
            "--pattern-size" -> config = config.copy(bgPatternSize = nxt().toDouble())
            "--gradient-mask" -> config = config.copy(gradientMask = nxt())
            "--gradient-center" -> config = config.copy(gradientCenter = nxt())
            "--gradient-edge" -> config = config.copy(gradientEdge = nxt())
            "--micro-text" -> config = config.copy(microText = nxt())
            "--micro-size" -> config = config.copy(microTextSize = nxt().toDouble())
            "--micro-color" -> config = config.copy(microTextColor = nxt())
            "--micro-path" -> config = config.copy(microTextPath = nxt())
            "-h", "--help" -> {
                printHelp()
                exitProcess(0)
            }
            else -> error("Unknown argument: ${args[i]}")
        }
        i++
    }
    
    return config
}

private fun readInput(inputFile: String?, encoding: String): ByteArray {
    val text = if (inputFile != null) {
        File(inputFile).readText()
    } else {
        generateSequence(::readLine).joinToString("\n")
    }
    
    return when (encoding.lowercase()) {
        "latin1" -> QrEncoding.fromLatin1(text)
        "base64" -> QrEncoding.fromBase64(text)
        "utf8" -> QrEncoding.fromUtf8(text)
        else -> error("Unknown encoding: $encoding")
    }
}

private fun generateQr(data: ByteArray, config: CliConfig): String {
    val qrBuilder = QRCode.custom()
        .size(config.width)
        .height(config.height)
        .margin(config.margin)
        .withColor(config.fg)
        .withBackground(config.bg)
        .errorCorrection(parseErrorCorrection(config.ec))
    
    // Dot style
    qrBuilder.dotStyle {
        type = when (config.dots.lowercase()) {
            "square" -> DotType.SQUARE
            "classy" -> DotType.CLASSY
            "rounded" -> DotType.ROUNDED
            "extra-rounded" -> DotType.EXTRA_ROUNDED
            "classy-rounded" -> DotType.CLASSY_ROUNDED
            else -> DotType.CIRCLE
        }
    }
    
    // Logo
    config.logo?.let { logoUrl ->
        qrBuilder.centerImage(logoUrl, config.logoSize)
    }
    
    // Corner locators
    config.cornerStyle?.let { style ->
        qrBuilder.cornerLocator {
            color = config.cornerColor
            when (style.lowercase()) {
                "square" -> square()
                "circle" -> circle()
                "rounded" -> rounded()
                "classy" -> classy()
            }
        }
    }
    
    // Advanced effects
    if (config.moduleOutline) {
        qrBuilder.moduleOutline(config.moduleOutlineColor, config.moduleOutlineWidth)
    }
    
    if (config.quietZone) {
        qrBuilder.quietZoneAccent(config.quietZoneColor, config.quietZoneWidth, config.quietZoneDash)
    }
    
    if (config.dropShadow) {
        qrBuilder.dropShadow(config.shadowBlur, config.shadowOpacity, config.shadowX, config.shadowY)
    }
    
    config.bgPattern?.let { pattern ->
        qrBuilder.backgroundPattern {
            when (pattern.lowercase()) {
                "dots" -> dots()
                "grid" -> grid()
                "diagonal" -> diagonal()
                "hexagon" -> hexagon()
            }
            color = config.bgPatternColor
            opacity = config.bgPatternOpacity
            size = config.bgPatternSize
        }
    }
    
    config.gradientMask?.let { mask ->
        qrBuilder.gradientMasking {
            when (mask.lowercase()) {
                "concentric" -> {
                    if (config.gradientCenter != null && config.gradientEdge != null) {
                        concentric(config.gradientCenter, config.gradientEdge)
                    }
                }
                "radial" -> {
                    if (config.gradientCenter != null && config.gradientEdge != null) {
                        radial(config.gradientCenter, config.gradientEdge)
                    }
                }
                "linear" -> {
                    if (config.gradientCenter != null && config.gradientEdge != null) {
                        linear(config.gradientCenter, config.gradientEdge)
                    }
                }
            }
        }
    }
    
    config.microText?.let { text ->
        qrBuilder.microTypography(text) {
            fontSize = config.microTextSize
            color = config.microTextColor
            when (config.microTextPath.lowercase()) {
                "circular" -> circular()
                "top" -> top()
                "bottom" -> bottom()
            }
        }
    }
    
    return qrBuilder.buildSvgFromBytes(data)
}

private fun parseErrorCorrection(ec: String): Ecc {
    return when (ec.lowercase()) {
        "l" -> Ecc.LOW
        "m" -> Ecc.MEDIUM
        "q" -> Ecc.QUARTILE
        "h" -> Ecc.HIGH
        else -> Ecc.QUARTILE
    }
}

private fun writeOutput(svg: String, outputFile: String?) {
    if (outputFile != null) {
        File(outputFile).writeText(svg)
    } else {
        print(svg)
    }
}

private fun printHelp() {
    println("""
QRGen CLI - Advanced QR Code Generator

Usage:
  echo "text" | qrgen-cli [options]
  qrgen-cli --input data.txt [options]

Basic Options:
  -i, --input <file>       Input file (default: stdin)
  -o, --output <file>      Output SVG file (default: stdout)
  --enc <utf8|latin1|base64> Encoding of input (default: utf8)
  --width <px>             SVG canvas width (default: 512)
  --height <px>            SVG canvas height (default: 512)
  --size <px>              Set both width and height
  --margin <px>            Margin around QR (default: 16)
  --dots <style>           Dot style: circle|square|classy|rounded|extra-rounded|classy-rounded
  --fg <color>             Foreground color (default: #000000)
  --bg <color|null>        Background color (default: #ffffff)
  --ec <l|m|q|h>           Error correction level (default: q)

Logo & Corners:
  --logo <url>             URL or data-URI for center image
  --logo-size <0.0-1.0>    Center image size ratio (default: 0.2)
  --corner-style <style>   Corner locator: square|circle|rounded|classy
  --corner-color <color>   Locator color (default: #000000)

Advanced Effects:
  --module-outline         Add subtle outline to modules
  --outline-color <color>  Module outline color (default: #111111)
  --outline-width <px>     Module outline width (default: 0.5)
  --quiet-zone             Add accent border around quiet zone
  --quiet-color <color>    Quiet zone border color (default: #444444)
  --quiet-width <px>       Quiet zone border width (default: 1.0)
  --quiet-dash <pattern>   Dash pattern for quiet zone (default: "4 4")
  --drop-shadow            Add subtle drop shadow effect
  --shadow-blur <px>       Shadow blur radius (default: 1.0)
  --shadow-opacity <0-1>   Shadow opacity (default: 0.2)
  --shadow-x <px>          Shadow X offset (default: 0.0)
  --shadow-y <px>          Shadow Y offset (default: 0.0)

Background Patterns:
  --bg-pattern <type>      Background pattern: dots|grid|diagonal|hexagon
  --pattern-color <color>  Pattern color (default: #f0f0f0)
  --pattern-opacity <0-1>  Pattern opacity (default: 0.02)
  --pattern-size <px>      Pattern size (default: 4.0)

Gradient Effects:
  --gradient-mask <type>   Gradient masking: concentric|radial|linear
  --gradient-center <color> Center color for gradient masking
  --gradient-edge <color>  Edge color for gradient masking

Typography:
  --micro-text <text>      Micro text around border
  --micro-size <px>        Micro text font size (default: 8.0)
  --micro-color <color>    Micro text color (default: #666666)
  --micro-path <path>      Text path: circular|top|bottom (default: circular)

  -h, --help               Show this help

Examples:
  # Basic QR code
  echo "Hello World" | qrgen-cli --enc utf8

  # Styled QR with advanced features
  echo "Advanced QR" | qrgen-cli --enc utf8 \\
    --dots extra-rounded --fg "#2ecc71" \\
    --drop-shadow --quiet-zone \\
    --micro-text "Secure • Verified"

  # QR with background pattern and gradient masking
  echo "Pattern QR" | qrgen-cli --enc utf8 \\
    --bg-pattern dots --gradient-mask concentric \\
    --gradient-center "#000" --gradient-edge "#666"

For library usage, see the Kotlin DSL documentation.
""".trimIndent())
} 