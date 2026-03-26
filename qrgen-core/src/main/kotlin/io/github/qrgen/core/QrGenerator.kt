package io.github.qrgen.core

import io.nayuki.qrcodegen.QrCode
import io.nayuki.qrcodegen.QrSegment
import kotlin.math.abs
import kotlin.math.sqrt

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

data class ModuleRegion(val rowStart: Int, val rowEnd: Int, val colStart: Int, val colEnd: Int) {
    fun contains(row: Int, col: Int): Boolean {
        return row in rowStart..rowEnd && col in colStart..colEnd
    }
}

/** QR code analysis utilities **/
object QrAnalysis {
    private val alignmentPatternPositions = mapOf(
        1 to intArrayOf(),
        2 to intArrayOf(6, 18),
        3 to intArrayOf(6, 22),
        4 to intArrayOf(6, 26),
        5 to intArrayOf(6, 30),
        6 to intArrayOf(6, 34),
        7 to intArrayOf(6, 22, 38),
        8 to intArrayOf(6, 24, 42),
        9 to intArrayOf(6, 26, 46),
        10 to intArrayOf(6, 28, 50),
        11 to intArrayOf(6, 30, 54),
        12 to intArrayOf(6, 32, 58),
        13 to intArrayOf(6, 34, 62),
        14 to intArrayOf(6, 26, 46, 66),
        15 to intArrayOf(6, 26, 48, 70),
        16 to intArrayOf(6, 26, 50, 74),
        17 to intArrayOf(6, 30, 54, 78),
        18 to intArrayOf(6, 30, 56, 82),
        19 to intArrayOf(6, 30, 58, 86),
        20 to intArrayOf(6, 34, 62, 90),
        21 to intArrayOf(6, 28, 50, 72, 94),
        22 to intArrayOf(6, 26, 50, 74, 98),
        23 to intArrayOf(6, 30, 54, 78, 102),
        24 to intArrayOf(6, 28, 54, 80, 106),
        25 to intArrayOf(6, 32, 58, 84, 110),
        26 to intArrayOf(6, 30, 58, 86, 114),
        27 to intArrayOf(6, 34, 62, 90, 118),
        28 to intArrayOf(6, 26, 50, 74, 98, 122),
        29 to intArrayOf(6, 30, 54, 78, 102, 126),
        30 to intArrayOf(6, 26, 52, 78, 104, 130),
        31 to intArrayOf(6, 30, 56, 82, 108, 134),
        32 to intArrayOf(6, 34, 60, 86, 112, 138),
        33 to intArrayOf(6, 30, 58, 86, 114, 142),
        34 to intArrayOf(6, 34, 62, 90, 118, 146),
        35 to intArrayOf(6, 30, 54, 78, 102, 126, 150),
        36 to intArrayOf(6, 24, 50, 76, 102, 128, 154),
        37 to intArrayOf(6, 28, 54, 80, 106, 132, 158),
        38 to intArrayOf(6, 32, 58, 84, 110, 136, 162),
        39 to intArrayOf(6, 26, 54, 82, 110, 138, 166),
        40 to intArrayOf(6, 30, 58, 86, 114, 142, 170)
    )

    fun isFinderPattern(row: Int, col: Int, size: Int): Boolean {
        return (row < 7 && col < 7) ||
            (row < 7 && col >= size - 7) ||
            (row >= size - 7 && col < 7)
    }

    fun alignmentCenters(version: Int): List<Pair<Int, Int>> {
        val positions = alignmentPatternPositions[version] ?: return emptyList()
        if (positions.isEmpty()) return emptyList()

        val centers = mutableListOf<Pair<Int, Int>>()
        for (row in positions) {
            for (col in positions) {
                val overlapsTopLeft = row == 6 && col == 6
                val overlapsTopRight = row == 6 && col == positions.last()
                val overlapsBottomLeft = row == positions.last() && col == 6
                if (overlapsTopLeft || overlapsTopRight || overlapsBottomLeft) continue
                centers += row to col
            }
        }
        return centers
    }

    fun alignmentRegions(version: Int): List<ModuleRegion> {
        return alignmentCenters(version).map { (row, col) ->
            ModuleRegion(row - 2, row + 2, col - 2, col + 2)
        }
    }

    fun isAlignmentPattern(row: Int, col: Int, version: Int): Boolean {
        return alignmentRegions(version).any { it.contains(row, col) }
    }

    fun isTimingPattern(row: Int, col: Int): Boolean {
        return (row == 6 && col >= 8) || (col == 6 && row >= 8)
    }

    fun shouldDrawModule(
        row: Int,
        col: Int,
        qrResult: QrResult,
        centerX: Double,
        centerY: Double,
        moduleSize: Double
    ): Boolean {
        val size = qrResult.size
        val config = qrResult.config

        if (config.locators.enabled && isFinderPattern(row, col, size)) {
            return false
        }

        if (config.alignmentPatterns.enabled && isAlignmentPattern(row, col, qrResult.qrCode.version)) {
            return false
        }

        config.logo.holeRadiusPx?.let { holeRadius ->
            val moduleX = col * moduleSize + moduleSize / 2
            val moduleY = row * moduleSize + moduleSize / 2
            val distance = sqrt(
                (moduleX - centerX) * (moduleX - centerX) +
                    (moduleY - centerY) * (moduleY - centerY)
            )
            if (distance < holeRadius) {
                return false
            }
        }

        return true
    }

    fun moduleNeighbors(modules: Array<BooleanArray>, row: Int, col: Int): ModuleNeighbors {
        fun on(r: Int, c: Int): Boolean {
            if (r !in modules.indices) return false
            if (c !in modules[r].indices) return false
            return modules[r][c]
        }

        return ModuleNeighbors(
            top = on(row - 1, col),
            right = on(row, col + 1),
            bottom = on(row + 1, col),
            left = on(row, col - 1)
        )
    }

    fun scaledModuleBounds(
        x: Double,
        y: Double,
        dot: Double,
        modules: ModuleOptions,
        row: Int,
        col: Int
    ): ModuleBounds {
        if (!modules.roundSize) {
            return ModuleBounds(x, y, dot, dot)
        }

        val baseScale = modules.sizeScale.coerceIn(0.6, 1.0)
        val variation = if ((row + col) % 2 == 0) 1.0 else 0.96
        val scaled = dot * (baseScale * variation)
        val inset = (dot - scaled) / 2.0
        return ModuleBounds(x + inset, y + inset, scaled, scaled)
    }
}

data class ModuleNeighbors(
    val top: Boolean,
    val right: Boolean,
    val bottom: Boolean,
    val left: Boolean
) {
    fun cornerRadius(corner: Corner, baseRadius: Double): Double {
        return when (corner) {
            Corner.TOP_LEFT -> if (top || left) 0.0 else baseRadius
            Corner.TOP_RIGHT -> if (top || right) 0.0 else baseRadius
            Corner.BOTTOM_RIGHT -> if (bottom || right) 0.0 else baseRadius
            Corner.BOTTOM_LEFT -> if (bottom || left) 0.0 else baseRadius
        }
    }
}

enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT }

data class ModuleBounds(val x: Double, val y: Double, val width: Double, val height: Double)

fun String.normalizedConfigKey(): String {
    return trim().replace(Regex("\\s+"), " ")
}

fun Double.isNearZero(): Boolean = abs(this) < 0.0001
