# QRForge4J Repository Structure

This document provides a comprehensive overview of the QRForge4J library repository structure.

## 📂 Repository Overview

```
QRForge4J/
├── 📁 qrgen-core/           # Core QR generation engine
├── 📁 qrgen-svg/            # SVG rendering with advanced styling
├── 📁 qrgen-png/            # High-quality PNG rendering
├── 📁 qrgen-dsl/            # Kotlin DSL for fluent API
├── 📁 qrgen-cli/            # Command-line interface
├── 📁 qrgen-test/           # Testing utilities and verification
├── 📁 qrgen-batch/          # Batch processing with coroutines
├── 📁 qrgen-spring-boot-starter/  # Spring Boot integration
├── 📁 qrgen-micronaut/      # Micronaut framework integration
├── 📁 qrgen-gradle-plugin/  # Gradle plugin for build-time generation
├── 📁 app/                  # Legacy CLI application (deprecated)
├── 📄 README.md             # Main project documentation
├── 📄 QUICKSTART.md         # Getting started guide
├── 📄 FEATURE_ANALYSIS.md   # Feature comparison with JS library
├── 📄 GITHUB_ROADMAP.md     # Development roadmap
├── 📄 LICENSE               # MIT License
└── 📄 REPOSITORY_STRUCTURE.md  # This file
```

## 🏗️ Module Architecture

### Core Modules

#### qrgen-core

**Purpose**: Foundation module with core QR generation logic

- **Package**: `io.github.qrgen.core`
- **Key Classes**:
  - `QrStyleConfig` - Main configuration class
  - `QrOptions` - QR code generation options
  - `LayoutOptions`, `ColorOptions`, `ModuleOptions` - Styling configurations
  - `LocatorOptions`, `GradientOptions`, `AdvancedOptions` - Advanced features
- **Dependencies**: `io.nayuki:qrcodegen`

#### qrgen-svg

**Purpose**: Advanced SVG rendering with rich styling capabilities

- **Package**: `io.github.qrgen.svg`
- **Key Classes**:
  - `SvgRenderer` - Main SVG generation engine
  - Supports: Drop shadows, gradients, patterns, module styles, corner locators
- **Dependencies**: `qrgen-core`

#### qrgen-png

**Purpose**: High-quality PNG rendering using Apache Batik

- **Package**: `io.github.qrgen.png`
- **Key Classes**:
  - `PngRenderer` - PNG generation with configurable DPI
- **Dependencies**: `qrgen-core`, `qrgen-svg`, Apache Batik

#### qrgen-dsl

**Purpose**: Kotlin DSL for elegant, fluent API usage

- **Package**: `io.github.qrgen.dsl`
- **Key Classes**:
  - `QrCodeBuilder` - Fluent builder API
  - Factory methods: `QRCode.ofCircles()`, `QRCode.ofSquares()`, etc.
- **Dependencies**: `qrgen-core`, `qrgen-svg`

### Utility Modules

#### qrgen-test

**Purpose**: Testing utilities and QR code verification

- **Package**: `io.github.qrgen.test`
- **Key Classes**:
  - `QrVerification` - ZXing-based verification
  - `PerformanceBenchmark` - Performance testing
  - `CompatibilityTest` - Cross-platform testing
- **Dependencies**: `qrgen-core`, ZXing

#### qrgen-batch

**Purpose**: Efficient batch processing with coroutines

- **Package**: `io.github.qrgen.batch`
- **Key Classes**:
  - `BatchProcessor` - Parallel QR generation
  - `ProgressReporter` - Progress monitoring
  - `MemoryMonitor` - Memory usage tracking
- **Dependencies**: `qrgen-core`, `qrgen-svg`, `qrgen-png`, Kotlin Coroutines

#### qrgen-cli

**Purpose**: Modern command-line interface

- **Package**: `io.github.qrgen.cli`
- **Key Classes**:
  - `QrGenCli` - Main CLI application
- **Dependencies**: `qrgen-dsl`, `qrgen-png`

### Framework Integration Modules

#### qrgen-spring-boot-starter

**Purpose**: Complete Spring Boot integration

- **Package**: `io.github.qrgen.spring`
- **Key Classes**:
  - `QrGenAutoConfiguration` - Auto-configuration
  - `QrController` - REST endpoints
  - `QrGenService` - Service layer
  - `QrGenProperties` - Configuration properties
- **Dependencies**: Spring Boot, `qrgen-batch`, `qrgen-png`

#### qrgen-micronaut

**Purpose**: Micronaut framework integration

- **Package**: `io.github.qrgen.micronaut`
- **Key Classes**:
  - `QrGenService` - Micronaut service
  - `QrController` - REST endpoints
- **Dependencies**: Micronaut, `qrgen-batch`, `qrgen-png`

#### qrgen-gradle-plugin

**Purpose**: Gradle plugin for build-time QR generation

- **Package**: `io.github.qrgen.gradle`
- **Key Classes**:
  - `QrGenPlugin` - Main plugin class
  - `QrGenExtension` - Configuration DSL
  - `QrGenTask` - Gradle task implementation
- **Dependencies**: Gradle API, `qrgen-core`, `qrgen-svg`, `qrgen-png`

## 🎯 Usage Patterns

### 1. Direct API Usage

```kotlin
// Core usage
val config = QrStyleConfig(...)
val renderer = SvgRenderer()
val svg = renderer.render("Hello World", config)
```

### 2. DSL Usage (Recommended)

```kotlin
// Fluent DSL
val qrSvg = QRCode.ofCircles()
    .size(600)
    .withColor("#2ecc71")
    .dropShadow()
    .buildSvg("https://example.com")
```

### 3. CLI Usage

```bash
./gradlew qrgen-cli:run --args="--data 'Hello World' --output hello.svg"
```

### 4. Framework Integration

```kotlin
// Spring Boot
@RestController
class MyController(private val qrGenService: QrGenService) {
    @GetMapping("/qr")
    fun generateQr() = qrGenService.generateQr("Hello World")
}
```

### 5. Build-time Generation

```kotlin
// Gradle plugin
qrgen {
    generateOnBuild = true
    qrCode {
        data("https://mysite.com")
        filename("site-qr")
        format("PNG")
        size(512)
    }
}
```

## 🏛️ Package Structure

All modules follow consistent package naming:

- **Root**: `io.github.qrgen`
- **Module-specific**: `io.github.qrgen.{module}`
- **Examples**:
  - `io.github.qrgen.core`
  - `io.github.qrgen.svg`
  - `io.github.qrgen.dsl`
  - `io.github.qrgen.spring`

## 📦 Distribution

### Maven Coordinates

```xml
<groupId>io.github.willmortimer</groupId>
<artifactId>qrgen-{module}</artifactId>
<version>1.0.0</version>
```

### Available Repositories

- **Maven Central** (primary)
- **GitHub Packages** (releases)
- **Local Maven** (development)

## 🔧 Build System

### Gradle Multi-Module Setup

- **Root**: Configuration and task orchestration
- **Modules**: Independent build configurations
- **Publishing**: Unified publishing to multiple repositories
- **Documentation**: Kotlin Dokka integration

### Key Gradle Tasks

```bash
./gradlew build              # Build all modules
./gradlew publishAllToMavenLocal  # Publish to local repository
./gradlew publishAll         # Publish to remote repositories
./gradlew dokkaHtmlMultiModule    # Generate API documentation
```

## 🧪 Testing Strategy

### Test Coverage by Module

- **qrgen-test**: Comprehensive verification utilities
- **Integration Tests**: Framework-specific testing
- **Performance Tests**: Benchmarking and optimization
- **Compatibility Tests**: Cross-platform verification

## 📋 Dependencies

### External Dependencies

- **Core QR Generation**: `io.nayuki:qrcodegen`
- **PNG Rendering**: Apache Batik suite
- **Testing**: ZXing, JUnit 5
- **Async Processing**: Kotlin Coroutines
- **Framework Integration**: Spring Boot, Micronaut
- **Build Tools**: Gradle, Kotlin Dokka

### Internal Dependencies

```
qrgen-core (foundation)
├── qrgen-svg (extends core)
├── qrgen-png (depends on svg + core)
├── qrgen-dsl (depends on svg + core)
├── qrgen-test (depends on core)
├── qrgen-batch (depends on png + svg + core)
├── qrgen-cli (depends on dsl + png)
├── qrgen-spring-boot-starter (depends on batch + png)
├── qrgen-micronaut (depends on batch + png)
└── qrgen-gradle-plugin (depends on png + svg + core)
```

## 🎨 Features by Module

### Advanced Features (8 beyond original JS library)

1. **Module outlines** (qrgen-svg)
2. **Quiet zone accents** (qrgen-svg)
3. **Drop shadows** (qrgen-svg)
4. **Pattern backgrounds** (qrgen-svg)
5. **Gradient masking** (qrgen-svg)
6. **Micro typography** (qrgen-svg)
7. **Custom alignment patterns** (qrgen-svg)
8. **Corner locator logos** (qrgen-svg)

### Enterprise Features

- **Batch processing** with coroutines (qrgen-batch)
- **Performance monitoring** (qrgen-test)
- **Framework integrations** (qrgen-spring-boot-starter, qrgen-micronaut)
- **Build-time generation** (qrgen-gradle-plugin)
- **High-quality PNG rendering** (qrgen-png)

## 📚 Documentation Structure

- **README.md**: Project overview and quick start
- **QUICKSTART.md**: Comprehensive usage guide
- **FEATURE_ANALYSIS.md**: Comparison with JavaScript library
- **GITHUB_ROADMAP.md**: Development milestones
- **API Documentation**: Generated with Kotlin Dokka
- **REPOSITORY_STRUCTURE.md**: This architectural overview

---

_This structure represents a complete, production-ready library ecosystem that surpasses the original JavaScript library in features, performance, and developer experience._
