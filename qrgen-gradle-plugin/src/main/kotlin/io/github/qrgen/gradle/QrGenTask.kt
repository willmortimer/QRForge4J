package io.github.qrgen.gradle

import io.github.qrgen.core.*
import io.github.qrgen.svg.SvgRenderer
import io.github.qrgen.png.PngRenderer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import java.io.File

/**
 * Gradle task for generating multiple QR codes
 */
abstract class QrGenTask : DefaultTask() {
    
    @get:Input
    abstract val qrConfigs: ListProperty<QrCodeConfig>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun generateQrCodes() {
        val outputDirectory = outputDir.get().asFile
        outputDirectory.mkdirs()
        
        qrConfigs.get().forEach { config ->
            try {
                generateSingleQr(config, outputDirectory)
                logger.info("Generated QR code: ${config.filename}.${config.format.lowercase()}")
            } catch (e: Exception) {
                logger.error("Failed to generate QR code for '${config.data}': ${e.message}")
                throw e
            }
        }
        
        logger.lifecycle("Generated ${qrConfigs.get().size} QR codes in $outputDirectory")
    }
    
    private fun generateSingleQr(config: QrCodeConfig, outputDir: File) {
        val qrStyleConfig = buildQrStyleConfig(config)
        val format = QrFormat.valueOf(config.format.uppercase())
        
        val result = when (format) {
            QrFormat.SVG -> {
                val renderer = SvgRenderer()
                val svg = renderer.render(config.data, qrStyleConfig)
                QrResult(svg.toByteArray(), "image/svg+xml", "${config.filename}.svg")
            }
            QrFormat.PNG -> {
                val renderer = PngRenderer()
                val png = renderer.render(config.data, qrStyleConfig)
                QrResult(png, "image/png", "${config.filename}.png")
            }
        }
        
        val outputFile = File(outputDir, result.filename)
        outputFile.writeBytes(result.data)
    }
    
    private fun buildQrStyleConfig(config: QrCodeConfig): QrStyleConfig {
        return QrStyleConfig(
            layout = LayoutOptions(
                width = config.width,
                height = config.height,
                margin = config.margin
            ),
            colors = ColorOptions(
                foreground = config.foregroundColor,
                background = config.backgroundColor
            ),
            modules = ModuleOptions(
                type = DotType.valueOf(config.moduleType.uppercase())
            ),
            locators = LocatorOptions(
                shape = parseLocatorShape(config.cornerStyle)
            ),
            logo = if (config.withLogo != null) {
                LogoOptions(href = config.withLogo)
            } else LogoOptions(),
            gradient = if (config.withGradient && config.gradientColors.isNotEmpty()) {
                GradientOptions(
                    type = GradientType.LINEAR,
                    stops = config.gradientColors.mapIndexed { index, color ->
                        ColorStop(index.toDouble() / (config.gradientColors.size - 1), color)
                    }
                )
            } else GradientOptions(),
            advanced = AdvancedOptions(
                dropShadow = if (config.withDropShadow) DropShadow(enabled = true) else null,
                backgroundPattern = config.withPattern?.let { 
                    BackgroundPattern(
                        enabled = true, 
                        type = PatternType.valueOf(it.uppercase())
                    )
                }
            ),
            qrOptions = QrOptions(
                ecc = when (config.errorCorrection.uppercase()) {
                    "LOW", "L" -> io.nayuki.qrcodegen.QrCode.Ecc.LOW
                    "MEDIUM", "M" -> io.nayuki.qrcodegen.QrCode.Ecc.MEDIUM
                    "QUARTILE", "Q" -> io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
                    "HIGH", "H" -> io.nayuki.qrcodegen.QrCode.Ecc.HIGH
                    else -> io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
                }
            )
        )
    }
    
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
 * Result of QR code generation
 */
data class QrResult(
    val data: ByteArray,
    val contentType: String,
    val filename: String
) 