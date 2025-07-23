package io.github.qrgen.spring

import io.github.qrgen.batch.QrBatchProcessor
import io.github.qrgen.core.DefaultQrGenerator
import io.github.qrgen.dsl.QRCode
import io.github.qrgen.png.BatikPngRenderer
import io.github.qrgen.svg.DefaultSvgRenderer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring Boot auto-configuration for QRGen library
 */
@AutoConfiguration
@ConditionalOnClass(QRCode::class)
@EnableConfigurationProperties(QrGenProperties::class)
class QrGenAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    fun qrGenerator(): DefaultQrGenerator = DefaultQrGenerator()
    
    @Bean
    @ConditionalOnMissingBean
    fun svgRenderer(): DefaultSvgRenderer = DefaultSvgRenderer()
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = ["org.apache.batik.transcoder.image.PNGTranscoder"])
    fun pngRenderer(): BatikPngRenderer = BatikPngRenderer()
    
    @Bean
    @ConditionalOnMissingBean
    fun batchProcessor(): QrBatchProcessor = QrBatchProcessor()
    
    @Bean
    @ConditionalOnMissingBean
    fun qrGenService(
        qrGenerator: DefaultQrGenerator,
        svgRenderer: DefaultSvgRenderer,
        pngRenderer: BatikPngRenderer?,
        batchProcessor: QrBatchProcessor,
        properties: QrGenProperties
    ): QrGenService = QrGenService(qrGenerator, svgRenderer, pngRenderer, batchProcessor, properties)
}

/**
 * Configuration class for REST endpoints
 */
@Configuration
@ConditionalOnClass(name = ["org.springframework.web.bind.annotation.RestController"])
class QrGenWebConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    fun qrController(qrGenService: QrGenService): QrController = QrController(qrGenService)
} 