package io.github.qrgen.gradle

import io.github.qrgen.core.*
import io.github.qrgen.svg.SvgRenderer
import io.github.qrgen.png.PngRenderer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import java.io.File

/**
 * Gradle task for generating a single QR code with command line options
 * 
 * Usage: ./gradlew generateQrCode --data="Hello World" --filename="hello" --format=PNG
 */
abstract class QrGenSingleTask : DefaultTask() {
    
    @get:Input
    @get:Option(option = "data", description = "Data to encode in the QR code")
    abstract val data: Property<String>
    
    @get:Input
    @get:Option(option = "filename", description = "Output filename (without extension)")
    abstract val filename: Property<String>
    
    @get:Input
    @get:Option(option = "format", description = "Output format (SVG or PNG)")
    abstract val format: Property<String>
    
    @get:Input
    @get:Option(option = "size", description = "Size in pixels (width=height)")
    abstract val size: Property<String>
    
    @get:Input
    @get:Option(option = "foreground", description = "Foreground color (hex)")
    abstract val foregroundColor: Property<String>
    
    @get:Input
    @get:Option(option = "background", description = "Background color (hex)")
    abstract val backgroundColor: Property<String>
    
    @get:Input
    @get:Option(option = "module-type", description = "Module type (CIRCLE, SQUARE, CLASSY, ROUNDED, etc.)")
    abstract val moduleType: Property<String>
    
    @get:Input
    @get:Option(option = "corner-style", description = "Corner locator style (SQUARE, CIRCLE, ROUNDED, CLASSY)")
    abstract val cornerStyle: Property<String>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    init {
        // Set default values
        filename.convention("qr")
        format.convention("SVG")
        size.convention("512")
        foregroundColor.convention("#000000")
        backgroundColor.convention("#ffffff")
        moduleType.convention("CIRCLE")
        cornerStyle.convention("SQUARE")
    }
    
    @TaskAction
    fun generateQrCode() {
        if (!data.isPresent) {
            logger.error("Data parameter is required. Use --data=\"Your text here\"")
            return
        }
        
        val outputDirectory = outputDir.get().asFile
        outputDirectory.mkdirs()
        
        try {
            val config = createConfigFromOptions()
            generateSingleQr(config, outputDirectory)
            logger.lifecycle("Generated QR code: ${outputDirectory}/${config.filename}.${config.format.lowercase()}")
        } catch (e: Exception) {
            logger.error("Failed to generate QR code: ${e.message}")
            throw e
        }
    }
    
    private fun createConfigFromOptions(): QrCodeConfig {
        val sizeValue = size.get().toIntOrNull() ?: 512
        
        return QrCodeConfig().apply {
            data = this@QrGenSingleTask.data.get()
            filename = this@QrGenSingleTask.filename.get()
            format = this@QrGenSingleTask.format.get()
            width = sizeValue
            height = sizeValue
            foregroundColor = this@QrGenSingleTask.foregroundColor.get()
            backgroundColor = this@QrGenSingleTask.backgroundColor.get()
            moduleType = this@QrGenSingleTask.moduleType.get()
            cornerStyle = this@QrGenSingleTask.cornerStyle.get()
        }
    }
    
    private fun generateSingleQr(config: QrCodeConfig, outputDir: File) {
        val qrStyleConfig = buildQrStyleConfig(config)
        val format = config.format.uppercase()
        
        val result = when (format) {
            "SVG" -> {
                val renderer = SvgRenderer()
                val svg = renderer.render(config.data, qrStyleConfig)
                QrResult(svg.toByteArray(), "image/svg+xml", "${config.filename}.svg")
            }
            "PNG" -> {
                val renderer = PngRenderer()
                val png = renderer.render(config.data, qrStyleConfig)
                QrResult(png, "image/png", "${config.filename}.png")
            }
            else -> {
                val renderer = SvgRenderer()
                val svg = renderer.render(config.data, qrStyleConfig)
                QrResult(svg.toByteArray(), "image/svg+xml", "${config.filename}.svg")
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
                margin = 16
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
            qrOptions = QrOptions(
                ecc = io.nayuki.qrcodegen.QrCode.Ecc.QUARTILE
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