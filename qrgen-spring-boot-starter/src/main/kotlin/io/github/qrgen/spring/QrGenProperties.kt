package io.github.qrgen.spring

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for QRGen Spring Boot integration
 */
@ConfigurationProperties(prefix = "qrgen")
data class QrGenProperties(
    /**
     * Default QR code configuration
     */
    val defaults: DefaultQrConfig = DefaultQrConfig(),
    
    /**
     * Web endpoint configuration
     */
    val web: WebConfig = WebConfig(),
    
    /**
     * Batch processing configuration
     */
    val batch: BatchConfig = BatchConfig(),
    
    /**
     * Security configuration
     */
    val security: SecurityConfig = SecurityConfig()
)

data class DefaultQrConfig(
    val width: Int = 512,
    val height: Int = 512,
    val margin: Int = 16,
    val errorCorrection: String = "QUARTILE",
    val foregroundColor: String = "#000000",
    val backgroundColor: String = "#ffffff",
    val format: String = "SVG"
)

data class WebConfig(
    val enabled: Boolean = true,
    val basePath: String = "/qr",
    val maxDataLength: Int = 2000,
    val enableCors: Boolean = true,
    val corsOrigins: List<String> = listOf("*"),
    val rateLimitEnabled: Boolean = true,
    val rateLimitPerMinute: Int = 60
)

data class BatchConfig(
    val enabled: Boolean = true,
    val maxBatchSize: Int = 1000,
    val parallelism: Int = Runtime.getRuntime().availableProcessors(),
    val memoryThresholdMB: Long = 512
)

data class SecurityConfig(
    val enableAuthentication: Boolean = false,
    val allowedOrigins: List<String> = listOf("*"),
    val maxRequestSize: String = "10MB"
) 