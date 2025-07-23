package io.github.qrgen.core

import io.nayuki.qrcodegen.QrCode
import io.nayuki.qrcodegen.QrSegment

/** Core QR code generation interface **/
interface QrGenerator {
    fun generateFromText(text: String, config: QrStyleConfig): QrResult
    fun generateFromBytes(data: ByteArray, config: QrStyleConfig): QrResult
}

/** QR generation result containing the matrix and metadata **/
data class QrResult(
    val qrCode: QrCode,
    val config: QrStyleConfig,
    val modules: Array<BooleanArray>,
    val size: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as QrResult
        
        if (qrCode != other.qrCode) return false
        if (config != other.config) return false
        if (!modules.contentDeepEquals(other.modules)) return false
        if (size != other.size) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = qrCode.hashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + modules.contentDeepHashCode()
        result = 31 * result + size
        return result
    }
}

/** Default implementation of QR generator using Nayuki's library **/
class DefaultQrGenerator : QrGenerator {
    
    override fun generateFromText(text: String, config: QrStyleConfig): QrResult {
        val bytes = text.toByteArray(Charsets.UTF_8)
        return generateFromBytes(bytes, config)
    }
    
    override fun generateFromBytes(data: ByteArray, config: QrStyleConfig): QrResult {
        val segment = QrSegment.makeBytes(data)
        val qrCode = QrCode.encodeSegments(
            listOf(segment),
            config.qrOptions.ecc,
            config.qrOptions.minVersion,
            config.qrOptions.maxVersion,
            config.qrOptions.mask,
            true
        )
        
        val size = qrCode.size
        val modules = Array(size) { row ->
            BooleanArray(size) { col ->
                qrCode.getModule(col, row)
            }
        }
        
        return QrResult(qrCode, config, modules, size)
    }
}

/** Encoding utilities **/
object QrEncoding {
    
    fun fromLatin1(text: String): ByteArray {
        return text.toByteArray(Charsets.ISO_8859_1)
    }
    
    fun fromBase64(base64: String): ByteArray {
        return java.util.Base64.getDecoder().decode(base64.trim())
    }
    
    fun fromUtf8(text: String): ByteArray {
        return text.toByteArray(Charsets.UTF_8)
    }
}

/** QR code analysis utilities **/
object QrAnalysis {
    
    fun isFinderPattern(row: Int, col: Int, size: Int): Boolean {
        return (row < 7 && col < 7) ||
               (row < 7 && col >= size - 7) ||
               (row >= size - 7 && col < 7)
    }
    
    fun isAlignmentPattern(row: Int, col: Int, size: Int): Boolean {
        // Simple approximation - actual alignment pattern positions depend on version
        val center = size / 2
        val alignmentSize = 5
        return (row >= center - alignmentSize/2 && row <= center + alignmentSize/2 &&
                col >= center - alignmentSize/2 && col <= center + alignmentSize/2)
    }
    
    fun isTimingPattern(row: Int, col: Int): Boolean {
        return (row == 6 && col >= 8) || (col == 6 && row >= 8)
    }
    
    fun shouldDrawModule(
        row: Int, 
        col: Int, 
        size: Int, 
        config: QrStyleConfig,
        centerX: Double,
        centerY: Double,
        moduleSize: Double
    ): Boolean {
        // Skip finder patterns if custom locators are used
        if (config.locators.shape != null && isFinderPattern(row, col, size)) {
            return false
        }
        
        // Skip modules in logo hole area
        config.logo.holeRadiusPx?.let { holeRadius ->
            val moduleX = col * moduleSize + moduleSize / 2
            val moduleY = row * moduleSize + moduleSize / 2
            val distance = kotlin.math.sqrt(
                (moduleX - centerX) * (moduleX - centerX) + 
                (moduleY - centerY) * (moduleY - centerY)
            )
            if (distance < holeRadius) {
                return false
            }
        }
        
        return true
    }
} 