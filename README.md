# QRForge4J

QRForge4J is a multi-module JVM QR generation library written in Kotlin. It ships a core QR engine, SVG/PNG/JPEG/PDF output, a Kotlin DSL, batch helpers, server integrations for Ktor, Spring Boot, and Micronaut, a CLI, a verification module, and a standalone Gradle plugin build.

## Status

- Main multi-module build: `./gradlew build`
- Standalone Gradle plugin build: `./gradlew -p qrgen-gradle-plugin build`
- Java baseline: 17
- Gradle wrapper: 9.4.0
- Main repo Kotlin plugin: 2.3.10

Project status details are in [`documents/STATE_OF_REPO.md`](./documents/STATE_OF_REPO.md).
Installation details are in [`documents/MAVEN_INSTALL.md`](./documents/MAVEN_INSTALL.md).

## Modules

- `qrgen-core`: core QR generation, config model, templates/profiles, request mapping, cache helpers
- `qrgen-svg`: SVG renderer with advanced styling
- `qrgen-png`: PNG and JPEG raster rendering
- `qrgen-pdf`: PDF output
- `qrgen-dsl`: Kotlin builder and fluent API
- `qrgen-cli`: command-line interface
- `qrgen-batch`: batch generation helpers
- `qrgen-test`: verification and test utilities
- `qrgen-ktor`: Ktor integration helpers and routes
- `qrgen-spring-boot-starter`: Spring Boot integration
- `qrgen-micronaut`: Micronaut integration
- `qrgen-gradle-plugin`: standalone Gradle plugin nested build

## Features

- Module styles: circle, square, classy, rounded, extra-rounded, classy-rounded
- Neighbor-aware rounded modules and `roundSize`-style dynamic sizing
- Corner locator styling with separate outer/inner shapes, per-corner overrides, and optional corner logos
- Custom alignment pattern styling: square, circle, diamond, star
- Background colors, rounded backgrounds, gradients, gradient masking, and background patterns
- Quiet zone accents, module outlines, drop shadows, and micro typography
- SVG animation presets: fade, pulse, draw-in
- Center logo support with hole carving
- JSON/YAML templates and named profile loading helpers
- Deep field-by-field template/profile merging for nested config overrides
- SVG, PNG, JPEG, and PDF output

## Local Development

With `mise`:

```bash
mise install
mise run build
mise run verify_integrations
mise run docs
mise run benchmarks
mise run build_plugin
mise run publish_local
```

With the Gradle wrapper:

```bash
./gradlew build
./gradlew -p qrgen-gradle-plugin build
```

`mise` manages the toolchain. The Gradle wrapper still downloads its own pinned distribution the first time it runs; that is normal wrapper behavior.

## Installation

Published modules use group `io.github.willmortimer` and artifact ids that follow the module names.

Detailed install guidance for GitHub Packages, `mavenLocal()`, and Ktor apps is in [`documents/MAVEN_INSTALL.md`](./documents/MAVEN_INSTALL.md).

GitHub Packages repository example:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/willmortimer/QRForge4J")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}
```

Kotlin app dependencies:

```kotlin
dependencies {
    implementation("io.github.willmortimer:qrgen-dsl:1.0.0")
    implementation("io.github.willmortimer:qrgen-ktor:1.0.0")
    implementation("io.github.willmortimer:qrgen-pdf:1.0.0")
}
```

## Usage

### Kotlin DSL

```kotlin
import io.github.qrgen.dsl.QRCode
import io.github.qrgen.core.LocatorFrameShape

val svg = QRCode.ofRoundedSquares()
    .size(640)
    .withColor("#111827")
    .backgroundCorners(28.0)
    .cornerLocators {
        rounded()
        logo("https://example.com/corner-logo.png")
        topRight {
            outer(LocatorFrameShape.DIAMOND)
        }
    }
    .alignmentPattern {
        star()
        color = "#0f766e"
    }
    .dotStyle {
        dynamicSize(0.92)
    }
    .animation {
        pulse()
        durationSeconds = 2.0
    }
    .buildSvg("https://example.com")
```

### Direct API

```kotlin
import io.github.qrgen.core.DefaultQrGenerator
import io.github.qrgen.core.QrStyleConfig
import io.github.qrgen.svg.DefaultSvgRenderer

val qr = DefaultQrGenerator().generateFromText("Hello QRForge4J", QrStyleConfig())
val svg = DefaultSvgRenderer().render(qr)
```

### Ktor

```kotlin
import io.github.qrgen.ktor.qrGenRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.module() {
    routing {
        qrGenRoutes("/qr")
    }
}
```

Routes exposed:

- `GET /qr/generate`
- `POST /qr/generate`
- `GET /qr/health`

### CLI

```bash
echo "https://example.com" | ./gradlew qrgen-cli:run --args="--format pdf --dots rounded --round-size --corner-style classy --alignment-shape star --output qr.pdf"
```

### Templates

```yaml
profile: kiosk
config:
  layout:
    width: 640
    height: 640
    margin: 24
  alignmentPatterns:
    enabled: true
    shape: STAR
```

Template and profile files merge field-by-field. Unspecified nested fields stay on the base profile, while explicitly provided lists replace the base list.

## Publishing

Published modules are configured for GitHub Packages. The release workflow lives at [`release.yml`](./.github/workflows/release.yml).

Required environment variables:

- `GITHUB_ACTOR`
- `GITHUB_TOKEN`

Optional Maven Central release secrets for GitHub Actions:

- `CENTRAL_PORTAL_USERNAME`
- `CENTRAL_PORTAL_PASSWORD`
- `SIGNING_KEY`
- `SIGNING_PASSWORD`

Local publishing:

```bash
./gradlew publishAllToMavenLocal
./gradlew -p qrgen-gradle-plugin publishToMavenLocal
```

Maven Central publishing:

```bash
./gradlew releaseToCentralPortal
./gradlew -p qrgen-gradle-plugin releaseToCentralPortal
```

API docs:

```bash
./gradlew verifyDocs
```

## Notes

- `app/` remains as legacy code and is not the published library surface.
- `ported-library/qr-code-styling-master/` is a vendored reference, not part of the published Kotlin modules.
- PDF output currently embeds rendered QR imagery into the PDF page. It is suitable for distribution and scanning tests, but it is not a true vector PDF renderer yet.

## License

MIT License. See [`LICENSE`](./LICENSE).
