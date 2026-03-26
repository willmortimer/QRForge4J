# Installing QRForge4J

This guide covers the three practical install paths for QRForge4J:

- consume published artifacts from GitHub Packages
- consume local unpublished artifacts from `mavenLocal()`
- prepare for future Maven Central publishing

## GitHub Packages

QRForge4J currently publishes to the GitHub Packages Maven registry for this repository:

`https://maven.pkg.github.com/willmortimer/QRForge4J`

GitHub Packages Maven installs require authentication. For a developer machine or CI job outside this repository, use a GitHub personal access token with package read access.

Gradle Kotlin DSL:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/willmortimer/QRForge4J")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
                ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull
                ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.github.willmortimer:qrgen-dsl:1.0.0")
    implementation("io.github.willmortimer:qrgen-ktor:1.0.0")
}
```

Suggested `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

## Local Development With `mavenLocal()`

If you want to use QRForge4J from another local Ktor app before publishing a GitHub release, publish the artifacts to your local Maven cache:

```bash
./gradlew publishAllToMavenLocal
./gradlew -p qrgen-gradle-plugin publishToMavenLocal
```

Then in the consuming app:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.willmortimer:qrgen-ktor:1.0.0")
    implementation("io.github.willmortimer:qrgen-dsl:1.0.0")
}
```

This path does not require any GitHub token.

## Ktor App Example

```kotlin
dependencies {
    implementation("io.github.willmortimer:qrgen-ktor:1.0.0")
}
```

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

## Maven Central

QRForge4J is not yet configured to publish to Maven Central.

To add Maven Central publishing later, the project will need:

- a verified Sonatype Central namespace for `io.github.willmortimer`
- Central publishing credentials from the Sonatype Portal
- signed artifacts
- Central-compatible publishing wiring in Gradle or via a release tool

Until that is added, GitHub Packages and `mavenLocal()` are the supported install paths.
