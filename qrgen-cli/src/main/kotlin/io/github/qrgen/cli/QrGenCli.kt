package io.github.qrgen.cli

import io.github.qrgen.core.DefaultQrGenerator
import io.github.qrgen.core.QrConfigIO
import io.github.qrgen.core.QrGenerateRequest
import io.github.qrgen.core.QrProfileRegistry
import io.github.qrgen.core.QrRequestMapper
import io.github.qrgen.core.QrStyleConfig
import io.github.qrgen.dsl.QRCode
import io.github.qrgen.pdf.QrPdfRenderer
import io.github.qrgen.png.BatikPngRenderer
import io.github.qrgen.svg.DefaultSvgRenderer
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        val config = parseArgs(args)
        val bytes = readInput(config.inputFile, config.encoding)
        val request = config.toRequest(String(bytes, Charsets.UTF_8))
        val profileRegistry = QrProfileRegistry()
        config.profileFile?.let {
            val document = QrConfigIO.readTemplate(File(it))
            profileRegistry.register(File(it).nameWithoutExtension, document.config)
        }
        val qrConfig = if (config.templateFile != null) {
            profileRegistry.merge(QrConfigIO.readTemplate(File(config.templateFile)))
        } else {
            QrRequestMapper.toConfig(request, profileRegistry)
        }
        val output = generateQr(request.data, qrConfig, config.format)
        writeOutput(output, config.outputFile)
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
    val format: String = "svg",
    val cornerStyle: String? = null,
    val cornerColor: String = "#000000",
    val cornerLogo: String? = null,
    val alignmentShape: String? = null,
    val alignmentColor: String? = null,
    val alignmentSizeRatio: Double = 0.9,
    val animationPreset: String? = null,
    val animationDuration: Double = 1.5,
    val roundSize: Boolean = false,
    val moduleScale: Double = 0.92,
    val backgroundCornerRadius: Double = 0.0,
    val templateFile: String? = null,
    val profileFile: String? = null
) {
    fun toRequest(data: String): QrGenerateRequest {
        return QrGenerateRequest(
            data = data,
            format = format.uppercase(),
            width = width,
            height = height,
            margin = margin,
            foregroundColor = fg,
            backgroundColor = bg,
            backgroundCornerRadius = backgroundCornerRadius,
            moduleType = dots.uppercase().replace('-', '_'),
            roundSize = roundSize,
            moduleScale = moduleScale,
            cornerStyle = cornerStyle,
            cornerColor = cornerColor,
            cornerLogo = cornerLogo,
            alignmentPatternShape = alignmentShape,
            alignmentPatternColor = alignmentColor,
            alignmentPatternSizeRatio = alignmentSizeRatio,
            animationPreset = animationPreset,
            animationDurationSeconds = animationDuration
        )
    }
}

private fun parseArgs(args: Array<String>): CliConfig {
    var config = CliConfig()
    var i = 0

    fun nxt(): String {
        if (++i >= args.size) error("Missing argument after ${args[i - 1]}")
        return args[i]
    }

    while (i < args.size) {
        when (args[i]) {
            "-i", "--input" -> config = config.copy(inputFile = nxt())
            "-o", "--output" -> config = config.copy(outputFile = nxt())
            "--enc" -> config = config.copy(encoding = nxt())
            "--width" -> config = config.copy(width = nxt().toInt())
            "--height" -> config = config.copy(height = nxt().toInt())
            "--size" -> nxt().toInt().also { config = config.copy(width = it, height = it) }
            "--margin" -> config = config.copy(margin = nxt().toInt())
            "--dots" -> config = config.copy(dots = nxt())
            "--fg" -> config = config.copy(fg = nxt())
            "--bg" -> config = config.copy(bg = nxt().let { if (it == "null") null else it })
            "--format" -> config = config.copy(format = nxt())
            "--corner-style" -> config = config.copy(cornerStyle = nxt())
            "--corner-color" -> config = config.copy(cornerColor = nxt())
            "--corner-logo" -> config = config.copy(cornerLogo = nxt())
            "--alignment-shape" -> config = config.copy(alignmentShape = nxt())
            "--alignment-color" -> config = config.copy(alignmentColor = nxt())
            "--alignment-size" -> config = config.copy(alignmentSizeRatio = nxt().toDouble())
            "--animation" -> config = config.copy(animationPreset = nxt())
            "--animation-duration" -> config = config.copy(animationDuration = nxt().toDouble())
            "--round-size" -> config = config.copy(roundSize = true)
            "--module-scale" -> config = config.copy(moduleScale = nxt().toDouble())
            "--background-corners" -> config = config.copy(backgroundCornerRadius = nxt().toDouble())
            "--template" -> config = config.copy(templateFile = nxt())
            "--profile" -> config = config.copy(profileFile = nxt())
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
    val text = if (inputFile != null) File(inputFile).readText() else generateSequence(::readLine).joinToString("\n")
    return when (encoding.lowercase()) {
        "latin1" -> text.toByteArray(Charsets.ISO_8859_1)
        "base64" -> java.util.Base64.getDecoder().decode(text.trim())
        else -> text.toByteArray(Charsets.UTF_8)
    }
}

private fun generateQr(data: String, config: QrStyleConfig, format: String): ByteArray {
    val generator = DefaultQrGenerator()
    val qrResult = generator.generateFromText(data, config)
    return when (format.lowercase()) {
        "png" -> BatikPngRenderer().render(qrResult)
        "jpeg", "jpg" -> BatikPngRenderer().renderJpeg(qrResult)
        "pdf" -> QrPdfRenderer().render(qrResult)
        else -> DefaultSvgRenderer().render(qrResult).toByteArray()
    }
}

private fun writeOutput(bytes: ByteArray, outputFile: String?) {
    if (outputFile != null) {
        File(outputFile).writeBytes(bytes)
    } else {
        print(bytes.toString(Charsets.UTF_8))
    }
}

private fun printHelp() {
    println(
        """
QRGen CLI

Usage:
  echo "text" | qrgen-cli [options]

Core:
  --format <svg|png|jpeg|pdf>
  --dots <circle|square|classy|rounded|extra-rounded|classy-rounded>
  --round-size
  --module-scale <0.6-1.0>
  --background-corners <px>

Locator and Alignment:
  --corner-style <square|circle|rounded|classy>
  --corner-color <color>
  --corner-logo <url>
  --alignment-shape <square|circle|diamond|star>
  --alignment-color <color>
  --alignment-size <ratio>

Templates:
  --template <file.(json|yaml|yml)>
  --profile <file.(json|yaml|yml)>

Animation:
  --animation <fade|pulse|draw-in>
  --animation-duration <seconds>
        """.trimIndent()
    )
}
