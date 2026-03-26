package io.github.qrgen.test

import io.github.qrgen.core.CacheOptions
import io.github.qrgen.core.DefaultQrGenerator
import io.github.qrgen.core.DotType
import io.github.qrgen.core.ModuleOptions
import io.github.qrgen.core.QrRenderCache
import io.github.qrgen.core.QrStyleConfig
import io.github.qrgen.core.qrCacheKey
import io.github.qrgen.pdf.QrPdfRenderer
import io.github.qrgen.png.BatikPngRenderer
import io.github.qrgen.svg.DefaultSvgRenderer
import java.io.File
import kotlin.system.measureNanoTime

data class BenchmarkScenarioResult(
    val scenario: String,
    val generationMillis: Double,
    val svgMillis: Double,
    val pngMillis: Double,
    val jpegMillis: Double,
    val pdfMillis: Double,
    val coldCacheMillis: Double,
    val warmCacheMillis: Double,
    val memoryBytesDelta: Long
)

fun main() {
    val results = runBenchmarks()
    val reportsDir = File("build/reports/benchmarks").apply { mkdirs() }
    val json = buildString {
        append("[\n")
        results.forEachIndexed { index, result ->
            append(
                """  {"scenario":"${result.scenario}","generationMillis":${"%.3f".format(result.generationMillis)},"svgMillis":${"%.3f".format(result.svgMillis)},"pngMillis":${"%.3f".format(result.pngMillis)},"jpegMillis":${"%.3f".format(result.jpegMillis)},"pdfMillis":${"%.3f".format(result.pdfMillis)},"coldCacheMillis":${"%.3f".format(result.coldCacheMillis)},"warmCacheMillis":${"%.3f".format(result.warmCacheMillis)},"memoryBytesDelta":${result.memoryBytesDelta}}"""
            )
            append(if (index == results.lastIndex) "\n" else ",\n")
        }
        append("]\n")
    }
    File(reportsDir, "benchmark-results.json").writeText(json)
    File(reportsDir, "benchmark-summary.txt").writeText(
        results.joinToString("\n") {
            "${it.scenario}: gen=${"%.2f".format(it.generationMillis)}ms svg=${"%.2f".format(it.svgMillis)}ms png=${"%.2f".format(it.pngMillis)}ms jpeg=${"%.2f".format(it.jpegMillis)}ms pdf=${"%.2f".format(it.pdfMillis)}ms coldCache=${"%.2f".format(it.coldCacheMillis)}ms warmCache=${"%.2f".format(it.warmCacheMillis)}ms memDelta=${it.memoryBytesDelta}"
        } + "\n"
    )
    println("Benchmark reports written to ${reportsDir.absolutePath}")
}

fun runBenchmarks(): List<BenchmarkScenarioResult> {
    val generator = DefaultQrGenerator()
    val svgRenderer = DefaultSvgRenderer()
    val pngRenderer = BatikPngRenderer()
    val pdfRenderer = QrPdfRenderer()
    val scenarios = listOf(
        "small_default" to QrStyleConfig(),
        "heavy_styling" to QrStyleConfig(modules = ModuleOptions(type = DotType.CLASSY_ROUNDED, roundSize = true, sizeScale = 0.92)),
        "cache_disabled" to QrStyleConfig(cache = CacheOptions(enabled = false, maxEntries = 0)),
        "cache_enabled" to QrStyleConfig(cache = CacheOptions(enabled = true, maxEntries = 64))
    )

    return scenarios.map { (name, config) ->
        val data = "Benchmark scenario $name"
        val startMemory = usedMemory()
        val qrResult = generator.generateFromText(data, config)
        val generationMillis = nanosToMillis(measureNanoTime { generator.generateFromText(data, config) })
        val svgMillis = nanosToMillis(measureNanoTime { svgRenderer.render(qrResult) })
        val pngMillis = nanosToMillis(measureNanoTime { pngRenderer.render(qrResult) })
        val jpegMillis = nanosToMillis(measureNanoTime { pngRenderer.renderJpeg(qrResult) })
        val pdfMillis = nanosToMillis(measureNanoTime { pdfRenderer.render(qrResult) })

        val cache = QrRenderCache(CacheOptions(enabled = true, maxEntries = 16))
        val key = qrCacheKey(data, config, "SVG")
        val coldCacheMillis = nanosToMillis(measureNanoTime { cache.getOrPut(key) { svgRenderer.render(qrResult).toByteArray() } })
        val warmCacheMillis = nanosToMillis(measureNanoTime { cache.getOrPut(key) { svgRenderer.render(qrResult).toByteArray() } })

        BenchmarkScenarioResult(
            scenario = name,
            generationMillis = generationMillis,
            svgMillis = svgMillis,
            pngMillis = pngMillis,
            jpegMillis = jpegMillis,
            pdfMillis = pdfMillis,
            coldCacheMillis = coldCacheMillis,
            warmCacheMillis = warmCacheMillis,
            memoryBytesDelta = usedMemory() - startMemory
        )
    }
}

private fun nanosToMillis(nanos: Long): Double = nanos / 1_000_000.0

private fun usedMemory(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.totalMemory() - runtime.freeMemory()
}
