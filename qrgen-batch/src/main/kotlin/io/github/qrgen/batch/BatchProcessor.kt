package io.github.qrgen.batch

import io.github.qrgen.core.*
import io.github.qrgen.dsl.*
import io.github.qrgen.png.*
import io.github.qrgen.svg.DefaultSvgRenderer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.measureTime

/** Batch processing configuration **/
data class BatchConfig(
    val parallelism: Int = Runtime.getRuntime().availableProcessors(),
    val chunkSize: Int = 100,
    val memoryThresholdMB: Long = 512,
    val enableProgressReporting: Boolean = true,
    val outputFormat: OutputFormat = OutputFormat.SVG,
    val outputDirectory: File? = null,
    val fileNamePattern: String = "qr_{index}_{hash}.{ext}"
)

enum class OutputFormat { SVG, PNG }

/** Batch processing result **/
data class BatchResult(
    val totalProcessed: Int,
    val successful: Int,
    val failed: Int,
    val processingTime: Duration,
    val averageTimePerQr: Duration,
    val outputFiles: List<File> = emptyList(),
    val errors: List<BatchError> = emptyList()
)

data class BatchError(
    val index: Int,
    val data: String,
    val error: Throwable
)

/** Progress monitoring **/
data class BatchProgress(
    val processed: Int,
    val total: Int,
    val currentChunk: Int,
    val totalChunks: Int,
    val percentage: Double = if (total > 0) (processed.toDouble() / total) * 100 else 0.0,
    val estimatedTimeRemaining: Duration? = null
)

/** Batch QR code generation with high performance and memory efficiency **/
class QrBatchProcessor(
    private val config: BatchConfig = BatchConfig()
) {
    private val generator = DefaultQrGenerator()
    private val svgRenderer = DefaultSvgRenderer()
    private val pngRenderer = BatikPngRenderer()
    
    private val processedCount = AtomicLong(0)
    private val memoryMonitor = MemoryMonitor(config.memoryThresholdMB)
    
    /**
     * Process a batch of QR codes with identical styling
     */
    suspend fun processBatch(
        data: List<String>,
        qrConfig: QrStyleConfig,
        progressCallback: ((BatchProgress) -> Unit)? = null
    ): BatchResult = coroutineScope {
        val startTime = System.currentTimeMillis()
        val totalCount = data.size
        val chunks = data.chunked(config.chunkSize)
        val errors = ConcurrentHashMap<Int, BatchError>()
        val outputFiles = ConcurrentHashMap<Int, File>()
        
        // Create output directory if needed
        config.outputDirectory?.mkdirs()
        
        val results = chunks.mapIndexed { chunkIndex, chunk ->
            async {
                processChunk(chunk, chunkIndex * config.chunkSize, qrConfig, errors, outputFiles, progressCallback, totalCount, chunks.size)
            }
        }.awaitAll()
        
        val totalProcessed = results.sum()
        val processingTime = Duration.parse("${System.currentTimeMillis() - startTime}ms")
        
        BatchResult(
            totalProcessed = totalProcessed,
            successful = totalProcessed - errors.size,
            failed = errors.size,
            processingTime = processingTime,
            averageTimePerQr = processingTime / totalCount,
            outputFiles = outputFiles.values.toList().sortedBy { it.name },
            errors = errors.values.toList().sortedBy { it.index }
        )
    }
    
    /**
     * Process with individual configurations per QR code
     */
    suspend fun processBatchWithConfigs(
        dataAndConfigs: List<Pair<String, QrStyleConfig>>,
        progressCallback: ((BatchProgress) -> Unit)? = null
    ): BatchResult = coroutineScope {
        val startTime = System.currentTimeMillis()
        val totalCount = dataAndConfigs.size
        val chunks = dataAndConfigs.chunked(config.chunkSize)
        val errors = ConcurrentHashMap<Int, BatchError>()
        val outputFiles = ConcurrentHashMap<Int, File>()
        
        // Create output directory if needed
        config.outputDirectory?.mkdirs()
        
        val results = chunks.mapIndexed { chunkIndex, chunk ->
            async {
                processChunkWithConfigs(chunk, chunkIndex * config.chunkSize, errors, outputFiles, progressCallback, totalCount, chunks.size)
            }
        }.awaitAll()
        
        val totalProcessed = results.sum()
        val processingTime = Duration.parse("${System.currentTimeMillis() - startTime}ms")
        
        BatchResult(
            totalProcessed = totalProcessed,
            successful = totalProcessed - errors.size,
            failed = errors.size,
            processingTime = processingTime,
            averageTimePerQr = processingTime / totalCount,
            outputFiles = outputFiles.values.toList().sortedBy { it.name },
            errors = errors.values.toList().sortedBy { it.index }
        )
    }
    
    /**
     * Process a flow of QR codes for streaming scenarios
     */
    fun processFlow(
        dataFlow: Flow<String>,
        qrConfig: QrStyleConfig
    ): Flow<QrResult> = dataFlow
        .buffer(config.chunkSize)
        .flowOn(Dispatchers.Default)
        .map { data ->
            generator.generateFromText(data, qrConfig)
        }
    
    /**
     * Template-based batch processing for variable data
     */
    suspend fun processTemplate(
        template: QrCodeBuilder.() -> Unit,
        variableData: List<String>,
        progressCallback: ((BatchProgress) -> Unit)? = null
    ): BatchResult {
        // Pre-compile template configuration
        val templateBuilder = QRCode.custom()
        templateBuilder.template()
        
        return processBatch(variableData, templateBuilder.build("").config, progressCallback)
    }
    
    private suspend fun processChunk(
        chunk: List<String>,
        startIndex: Int,
        qrConfig: QrStyleConfig,
        errors: ConcurrentHashMap<Int, BatchError>,
        outputFiles: ConcurrentHashMap<Int, File>,
        progressCallback: ((BatchProgress) -> Unit)?,
        totalCount: Int,
        totalChunks: Int
    ): Int = withContext(Dispatchers.Default) {
        val semaphore = Semaphore(config.parallelism)
        val processed = AtomicLong(0)
        
        chunk.mapIndexed { relativeIndex, data ->
            async {
                semaphore.withPermit {
                    val absoluteIndex = startIndex + relativeIndex
                    try {
                        // Memory pressure check
                        memoryMonitor.checkMemoryPressure()
                        
                        val qrResult = generator.generateFromText(data, qrConfig)
                        val output = when (config.outputFormat) {
                            OutputFormat.SVG -> svgRenderer.render(qrResult)
                            OutputFormat.PNG -> String(pngRenderer.render(qrResult), Charsets.ISO_8859_1)
                        }
                        
                        // Save to file if output directory specified
                        config.outputDirectory?.let { dir ->
                            val fileName = config.fileNamePattern
                                .replace("{index}", absoluteIndex.toString().padStart(6, '0'))
                                .replace("{hash}", data.hashCode().toString())
                                .replace("{ext}", config.outputFormat.name.lowercase())
                            
                            val file = File(dir, fileName)
                            when (config.outputFormat) {
                                OutputFormat.SVG -> file.writeText(output)
                                OutputFormat.PNG -> file.writeBytes(pngRenderer.render(qrResult))
                            }
                            outputFiles[absoluteIndex] = file
                        }
                        
                        processed.incrementAndGet()
                        
                        // Report progress
                        if (config.enableProgressReporting && progressCallback != null) {
                            val totalProcessed = processedCount.incrementAndGet()
                            progressCallback(
                                BatchProgress(
                                    processed = totalProcessed.toInt(),
                                    total = totalCount,
                                    currentChunk = startIndex / config.chunkSize + 1,
                                    totalChunks = totalChunks
                                )
                            )
                        }
                        
                        1
                    } catch (e: Exception) {
                        errors[absoluteIndex] = BatchError(absoluteIndex, data, e)
                        0
                    }
                }
            }
        }.awaitAll().sum()
    }
    
    private suspend fun processChunkWithConfigs(
        chunk: List<Pair<String, QrStyleConfig>>,
        startIndex: Int,
        errors: ConcurrentHashMap<Int, BatchError>,
        outputFiles: ConcurrentHashMap<Int, File>,
        progressCallback: ((BatchProgress) -> Unit)?,
        totalCount: Int,
        totalChunks: Int
    ): Int = withContext(Dispatchers.Default) {
        val semaphore = Semaphore(config.parallelism)
        val processed = AtomicLong(0)
        
        chunk.mapIndexed { relativeIndex, (data, qrConfig) ->
            async {
                semaphore.withPermit {
                    val absoluteIndex = startIndex + relativeIndex
                    try {
                        memoryMonitor.checkMemoryPressure()
                        
                        val qrResult = generator.generateFromText(data, qrConfig)
                        val output = when (config.outputFormat) {
                            OutputFormat.SVG -> svgRenderer.render(qrResult)
                            OutputFormat.PNG -> String(pngRenderer.render(qrResult), Charsets.ISO_8859_1)
                        }
                        
                        // Save to file if output directory specified
                        config.outputDirectory?.let { dir ->
                            val fileName = config.fileNamePattern
                                .replace("{index}", absoluteIndex.toString().padStart(6, '0'))
                                .replace("{hash}", data.hashCode().toString())
                                .replace("{ext}", config.outputFormat.name.lowercase())
                            
                            val file = File(dir, fileName)
                            when (config.outputFormat) {
                                OutputFormat.SVG -> file.writeText(output)
                                OutputFormat.PNG -> file.writeBytes(pngRenderer.render(qrResult))
                            }
                            outputFiles[absoluteIndex] = file
                        }
                        
                        processed.incrementAndGet()
                        
                        // Report progress
                        if (config.enableProgressReporting && progressCallback != null) {
                            val totalProcessed = processedCount.incrementAndGet()
                            progressCallback(
                                BatchProgress(
                                    processed = totalProcessed.toInt(),
                                    total = totalCount,
                                    currentChunk = startIndex / config.chunkSize + 1,
                                    totalChunks = totalChunks
                                )
                            )
                        }
                        
                        1
                    } catch (e: Exception) {
                        errors[absoluteIndex] = BatchError(absoluteIndex, data, e)
                        0
                    }
                }
            }
        }.awaitAll().sum()
    }
}

/** Memory monitoring to prevent OOM **/
class MemoryMonitor(private val thresholdMB: Long) {
    private val runtime = Runtime.getRuntime()
    
    fun checkMemoryPressure() {
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        if (usedMemory > thresholdMB) {
            System.gc() // Suggest garbage collection
            
            // Check again after GC
            val usedAfterGC = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            if (usedAfterGC > thresholdMB * 1.2) {
                throw OutOfMemoryError("Memory usage exceeds threshold: ${usedAfterGC}MB > ${thresholdMB}MB")
            }
        }
    }
    
    fun getMemoryUsage(): MemoryUsage {
        val total = runtime.totalMemory() / 1024 / 1024
        val free = runtime.freeMemory() / 1024 / 1024
        val used = total - free
        val max = runtime.maxMemory() / 1024 / 1024
        
        return MemoryUsage(used, total, free, max)
    }
}

data class MemoryUsage(
    val usedMB: Long,
    val totalMB: Long,
    val freeMB: Long,
    val maxMB: Long
) {
    val usagePercentage: Double = (usedMB.toDouble() / maxMB) * 100
}

/** Utility functions for common batch scenarios **/
object BatchUtils {
    
    /**
     * Generate QR codes for a CSV file with data in first column
     */
    suspend fun processCsvFile(
        csvFile: File,
        qrConfig: QrStyleConfig,
        outputDir: File,
        skipHeader: Boolean = true
    ): BatchResult {
        val lines = csvFile.readLines()
        val data = if (skipHeader) lines.drop(1) else lines
        val qrData = data.map { it.split(",")[0].trim() }
        
        val config = BatchConfig(
            outputDirectory = outputDir,
            outputFormat = OutputFormat.SVG
        )
        
        val processor = QrBatchProcessor(config)
        return processor.processBatch(qrData, qrConfig)
    }
    
    /**
     * Generate QR codes for URL list with progress reporting
     */
    suspend fun processUrlList(
        urls: List<String>,
        outputDir: File,
        progressCallback: (BatchProgress) -> Unit = {}
    ): BatchResult {
        val config = BatchConfig(
            outputDirectory = outputDir,
            outputFormat = OutputFormat.PNG,
            enableProgressReporting = true
        )
        
        val qrConfig = QrStyleConfig(
            layout = LayoutOptions(width = 400, height = 400),
            qrOptions = QrOptions(ecc = io.nayuki.qrcodegen.QrCode.Ecc.MEDIUM)
        )
        
        val processor = QrBatchProcessor(config)
        return processor.processBatch(urls, qrConfig, progressCallback)
    }
    
    /**
     * Benchmark batch processing performance
     */
    suspend fun benchmarkBatchPerformance(
        itemCount: Int = 1000,
        parallelismLevels: List<Int> = listOf(1, 2, 4, 8, 16)
    ): Map<Int, BatchResult> {
        val testData = (1..itemCount).map { "Test QR Code #$it" }
        val qrConfig = QrStyleConfig()
        
        val results = mutableMapOf<Int, BatchResult>()
        
        for (parallelism in parallelismLevels) {
            val config = BatchConfig(
                parallelism = parallelism,
                enableProgressReporting = false,
                outputDirectory = null // In-memory only
            )
            
            val processor = QrBatchProcessor(config)
            results[parallelism] = processor.processBatch(testData, qrConfig)
        }
        
        return results
    }
} 