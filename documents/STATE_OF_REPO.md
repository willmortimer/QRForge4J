# QRForge4J State Of Repo

QRForge4J is a buildable multi-module JVM QR generation project with library modules for core QR generation, SVG/PNG/JPEG/PDF rendering, Kotlin DSL usage, Ktor, Spring Boot, Micronaut, batch processing, verification utilities, a CLI, and a standalone Gradle plugin build.

## Modules

Included in the main Gradle build:

- `qrgen-core`
- `qrgen-svg`
- `qrgen-png`
- `qrgen-pdf`
- `qrgen-dsl`
- `qrgen-test`
- `qrgen-cli`
- `qrgen-batch`
- `qrgen-ktor`
- `qrgen-spring-boot-starter`
- `qrgen-micronaut`

Standalone nested build:

- `qrgen-gradle-plugin`

Legacy/reference code still present:

- `app/`
- `ported-library/qr-code-styling-master/`

## Verified Status

- `./gradlew build` succeeds for the main multi-module build.
- `./gradlew -p qrgen-gradle-plugin build` succeeds for the standalone Gradle plugin build.
- The repo now includes alignment pattern styling, richer per-corner locator styling, corner locator logos, rounded background support, SVG animation presets, JSON/YAML template loading helpers, deep template/profile merging, JPEG output, PDF output, and Ktor/Spring/Micronaut request mapping for the expanded config surface.
- `qrgen-test` now covers formal ZXing-based scannability checks, checked-in SVG-first regression goldens, and local benchmark entry points.
- The repository is configured for Maven publishing to GitHub Packages for published modules.
- `mise.toml` provides a reproducible local toolchain path based on Java 17 and Gradle 9.4.0.

## Remaining Cleanup

- Remove or archive `app/` if the old CLI is no longer needed.
- Keep expanding the checked-in regression set as more rendering features land.
- Keep the README as the primary public reference and avoid reintroducing roadmap-style status documents.
