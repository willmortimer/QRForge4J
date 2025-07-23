package io.github.qrgen.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Gradle extension for configuring QR code generation
 */
abstract class QrGenExtension @Inject constructor(objects: ObjectFactory) {
    
    /**
     * Whether to generate QR codes automatically during build
     */
    val generateOnBuild: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    
    /**
     * List of QR code configurations to generate
     */
    val qrCodes: ListProperty<QrCodeConfig> = objects.listProperty(QrCodeConfig::class.java).convention(emptyList())
    
    /**
     * Add a QR code configuration
     */
    fun qrCode(config: QrCodeConfig.() -> Unit) {
        val qrConfig = QrCodeConfig()
        qrConfig.config()
        qrCodes.add(qrConfig)
    }
}

/**
 * Configuration for a single QR code
 */
class QrCodeConfig {
    var data: String = ""
    var filename: String = "qr"
    var format: String = "SVG"
    var width: Int = 512
    var height: Int = 512
    var margin: Int = 16
    var foregroundColor: String = "#000000"
    var backgroundColor: String = "#ffffff"
    var moduleType: String = "CIRCLE"
    var cornerStyle: String = "SQUARE"
    var errorCorrection: String = "QUARTILE"
    var withLogo: String? = null
    var withGradient: Boolean = false
    var gradientColors: List<String> = emptyList()
    var withDropShadow: Boolean = false
    var withPattern: String? = null
    
    fun data(value: String) {
        data = value
    }
    
    fun filename(value: String) {
        filename = value
    }
    
    fun format(value: String) {
        format = value
    }
    
    fun size(width: Int, height: Int = width) {
        this.width = width
        this.height = height
    }
    
    fun colors(foreground: String, background: String = "#ffffff") {
        foregroundColor = foreground
        backgroundColor = background
    }
    
    fun style(moduleType: String = "CIRCLE", cornerStyle: String = "SQUARE") {
        this.moduleType = moduleType
        this.cornerStyle = cornerStyle
    }
    
    fun withLogo(href: String) {
        withLogo = href
    }
    
    fun withGradient(vararg colors: String) {
        withGradient = true
        gradientColors = colors.toList()
    }
    
    fun withDropShadow() {
        withDropShadow = true
    }
    
    fun withPattern(pattern: String) {
        withPattern = pattern
    }
} 