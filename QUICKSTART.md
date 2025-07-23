# QRGen Quickstart Guide

Welcome to QRGen - the most advanced QR code generation library for the JVM ecosystem! This guide will get you up and running in minutes.

## 🚀 Quick Start

### 1. Add Dependencies

#### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("io.github.qrgen:qrgen-dsl:1.0.0")
    implementation("io.github.qrgen:qrgen-svg:1.0.0")
    implementation("io.github.qrgen:qrgen-png:1.0.0") // Optional for PNG support
}
```

#### Maven
```xml
<dependencies>
    <dependency>
        <groupId>io.github.willmortimer</groupId>
        <artifactId>qrgen-dsl</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>io.github.willmortimer</groupId>
        <artifactId>qrgen-svg</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>io.github.willmortimer</groupId>
        <artifactId>qrgen-png</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### 2. Generate Your First QR Code

```kotlin
import io.github.qrgen.dsl.*

// Simple QR code
val svg = QRCode.simple().buildSvg("Hello, World!")

// Advanced QR code with styling
val styledSvg = QRCode.ofCircles()
    .size(600)
    .withColor("#2ecc71")
    .dropShadow()
    .buildSvg("https://github.com/qrgen")
```

## 📦 Module Overview

QRGen is organized into focused modules for different use cases:

| Module | Purpose | When to Use |
|--------|---------|-------------|
| `qrgen-core` | Core QR generation | Always required |
| `qrgen-svg` | SVG rendering | For vector graphics |
| `qrgen-png` | PNG rendering with Apache Batik | For high-quality raster images |
| `qrgen-dsl` | Kotlin DSL | For elegant configuration |
| `qrgen-batch` | Batch processing | For high-volume generation |
| `qrgen-test` | ZXing testing utilities | For verification |
| `qrgen-spring-boot-starter` | Spring Boot integration | For web applications |
| `qrgen-cli` | Command-line interface | For scripts and automation |

## 🎨 Styling Examples

### Basic Configurations

```kotlin
// Different module styles
val circles = QRCode.ofCircles().buildSvg("Round modules")
val squares = QRCode.ofSquares().buildSvg("Square modules") 
val classy = QRCode.ofClassy().buildSvg("Classy modules")
val rounded = QRCode.ofRounded().buildSvg("Rounded corners")

// Size and colors
val customized = QRCode.custom()
    .size(400, 400)
    .margin(20)
    .withColor("#e74c3c")
    .backgroundColor("#ecf0f1")
    .buildSvg("Custom styling")
```

### Advanced Features

```kotlin
// Drop shadows and effects
val withEffects = QRCode.ofCircles()
    .dropShadow(color = "#3498db", blur = 8.0, offset = 4.0)
    .moduleOutline(color = "#2c3e50", width = 0.5)
    .buildSvg("Advanced effects")

// Background patterns
val withPattern = QRCode.ofSquares()
    .backgroundPattern { dots(spacing = 20.0, radius = 2.0, color = "#bdc3c7") }
    .buildSvg("Pattern background")

// Gradient fills
val withGradient = QRCode.custom()
    .gradientFill { linear(from = "#3498db", to = "#e74c3c", angle = 45.0) }
    .buildSvg("Gradient modules")

// Typography accents
val withText = QRCode.ofCircles()
    .microTypography("Secure QR Code") { circular(radius = 280.0) }
    .buildSvg("Text accent")
```

## 🏗️ Framework Integration

### Spring Boot

Add the starter dependency:
```kotlin
implementation("io.github.qrgen:qrgen-spring-boot-starter:1.0.0")
```

Configure in `application.yml`:
```yaml
qrgen:
  defaults:
    width: 512
    height: 512
    error-correction: QUARTILE
  web:
    enabled: true
    base-path: /api/qr
    max-data-length: 2000
  batch:
    max-batch-size: 1000
    parallelism: 8
```

Use in your controller:
```kotlin
@RestController
class MyController(private val qrGenService: QrGenService) {
    
    @GetMapping("/my-qr")
    fun generateQr(@RequestParam data: String): ResponseEntity<ByteArray> {
        val response = qrGenService.generateQr(data, QrFormat.SVG)
        return ResponseEntity.ok()
            .header("Content-Type", response.contentType)
            .body(response.data)
    }
}
```

Available endpoints:
- `GET /api/qr/generate?data=Hello&format=SVG` - Simple generation
- `POST /api/qr/generate` - Advanced generation with JSON config
- `POST /api/qr/batch` - Batch processing
- `GET /api/qr/health` - Health check

### Micronaut (Coming Soon)

```kotlin
implementation("io.github.qrgen:qrgen-micronaut:1.0.0")
```

## 🚄 High-Performance Batch Processing

For generating thousands of QR codes efficiently:

```kotlin
import io.github.qrgen.batch.*
import kotlinx.coroutines.runBlocking

val processor = QrBatchProcessor(BatchConfig(
    parallelism = 16,
    chunkSize = 100,
    outputFormat = OutputFormat.PNG,
    outputDirectory = File("output")
))

runBlocking {
    val urls = (1..10000).map { "https://example.com/item/$it" }
    val result = processor.processBatch(urls, QrStyleConfig()) { progress ->
        println("Progress: ${progress.percentage}%")
    }
    
    println("Generated ${result.successful} QR codes in ${result.processingTime}")
}
```

## 🎯 PNG Rendering

High-quality PNG output with Apache Batik:

```kotlin
import io.github.qrgen.png.*

val qrResult = QRCode.ofCircles().build("PNG Test")

// High-DPI for print
val printPng = PngUtils.createPrintQuality(qrResult)

// Web-optimized
val webPng = PngUtils.createWebOptimized(qrResult)

// Retina displays
val retinaPng = PngUtils.createRetina(qrResult)

// Custom configuration
val renderer = BatikPngRenderer()
val customPng = renderer.render(qrResult, PngRenderConfig(
    dpi = 300f,
    antiAliasing = true,
    colorMode = ColorMode.RGBA
))
```

## 🧪 Testing and Verification

Verify your QR codes are scannable:

```kotlin
import io.github.qrgen.test.*

val verifier = QrVerifier()

// Test a single QR code
val result = verifier.verify("Test data", QrStyleConfig())
println("Scannable: ${result.isSuccessful}")
println("Content matches: ${result.contentMatches}")

// Test all module styles
val compatibilityResults = QrTestUtils.testAllModuleStyles()
compatibilityResults.forEach { (style, result) ->
    println("$style: ${if (result.isSuccessful) "✓" else "✗"}")
}

// Performance benchmarking
val metrics = QrTestUtils.benchmarkPerformance(iterations = 1000)
println("Average generation time: ${metrics.map { it.generationTime.inWholeMilliseconds }.average()}ms")
```

## 🛠️ Command Line Interface

Generate QR codes from the command line:

```bash
# Install via package manager or download jar
java -jar qrgen-cli-1.0.0.jar

# Basic usage
qrgen "Hello, World!" -o qr.svg

# Advanced styling
qrgen "https://github.com" \
  --style circles \
  --size 800x800 \
  --color "#e74c3c" \
  --background "#ecf0f1" \
  --drop-shadow \
  --output styled-qr.svg

# Batch processing
qrgen --batch urls.txt --output-dir qr-codes/ --format png --parallel 8
```

## 🔧 Low-Level API

For maximum control, use the core API directly:

```kotlin
import io.github.qrgen.core.*
import io.github.qrgen.svg.DefaultSvgRenderer

val generator = DefaultQrGenerator()
val renderer = DefaultSvgRenderer()

val config = QrStyleConfig(
    layout = LayoutOptions(width = 600, height = 600),
    modules = ModuleOptions(type = DotType.CIRCLE),
    colors = ColorOptions(foreground = "#2c3e50", background = "#ecf0f1"),
    qrOptions = QrOptions(ecc = io.nayuki.qrcodegen.QrCode.Ecc.HIGH)
)

val qrResult = generator.generateFromText("Low-level API", config)
val svg = renderer.render(qrResult)
```

## 📚 Documentation

- **API Docs**: [Generated with Dokka](./docs/api/)
- **Feature Analysis**: [FEATURE_ANALYSIS.md](./FEATURE_ANALYSIS.md)
- **Development Roadmap**: [GITHUB_ROADMAP.md](./GITHUB_ROADMAP.md)
- **Architecture Overview**: [README.md](./README.md)

## 🎉 Examples Gallery

### URL QR Code with Logo Space
```kotlin
val urlQr = QRCode.ofRounded()
    .size(500)
    .withColor("#1a73e8")
    .logo(holeRadiusPx = 50.0) // Space for logo
    .buildSvg("https://example.com")
```

### Branded QR Code
```kotlin
val brandedQr = QRCode.ofCircles()
    .gradientFill { radial(center = "#ff6b6b", edge = "#4ecdc4") }
    .cornerLocators { rounded() }
    .dropShadow(color = "#333", blur = 6.0)
    .buildSvg("Brand Message")
```

### High-Security QR Code
```kotlin
val secureQr = QRCode.custom()
    .errorCorrection(io.nayuki.qrcodegen.QrCode.Ecc.HIGH)
    .moduleOutline(color = "#e74c3c", width = 1.0)
    .microTypography("VERIFIED") { linear(y = 450.0) }
    .buildSvg("Sensitive Data")
```

### Performance Optimized
```kotlin
val fastQr = QRCode.ofSquares() // Fastest rendering
    .size(256) // Minimal size
    .margin(8) // Reduced margin
    .buildSvg("Fast Generation")
```

## 🔄 Migration from Other Libraries

### From ZXing
```kotlin
// ZXing
val writer = QRCodeWriter()
val matrix = writer.encode("data", BarcodeFormat.QR_CODE, 300, 300)

// QRGen
val svg = QRCode.simple().size(300).buildSvg("data")
```

### From QRGen (Old)
```kotlin
// Old QRGen
QRCode.from("text").to(ImageType.SVG).withSize(250, 250).file()

// New QRGen
QRCode.simple().size(250).buildSvg("text")
```

## 🚨 Troubleshooting

### Common Issues

**PNG rendering not working**
```kotlin
// Ensure Batik dependency is included
implementation("io.github.qrgen:qrgen-png:1.0.0")
```

**Out of memory with batch processing**
```kotlin
// Reduce batch size or increase heap
val config = BatchConfig(
    chunkSize = 50, // Reduced from default 100
    memoryThresholdMB = 256 // Reduced threshold
)
```

**QR codes not scanning**
```kotlin
// Use higher error correction
QRCode.custom()
    .errorCorrection(io.nayuki.qrcodegen.QrCode.Ecc.HIGH)
    .buildSvg("data")

// Or verify with testing utilities
val result = QrVerifier().verify("data", config)
println("Scannable: ${result.isSuccessful}")
```

## 🤝 Contributing

We welcome contributions! See our [Contributing Guide](CONTRIBUTING.md) for details.

- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/yourusername/qr-generator/issues)
- 💡 **Feature Requests**: [GitHub Discussions](https://github.com/yourusername/qr-generator/discussions)
- 📖 **Documentation**: Improve this guide or API docs
- 🧪 **Testing**: Add test cases or performance benchmarks

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

---

**Next Steps:**
- Explore the [feature comparison](FEATURE_ANALYSIS.md) with other libraries
- Check out [advanced examples](examples/) in the repository
- Join our [community discussions](https://github.com/yourusername/qr-generator/discussions)
- Star the project on [GitHub](https://github.com/yourusername/qr-generator) ⭐ 