# Installing QRForge4J

This guide covers the three practical install paths for QRForge4J:

- consume published artifacts from GitHub Packages
- consume local unpublished artifacts from `mavenLocal()`
- consume public artifacts from Maven Central once Central publishing is enabled

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

What `mavenLocal()` means:

- Gradle writes the built artifacts into your user-local Maven cache, usually under `~/.m2/repository/`
- another local project on the same Mac can resolve those artifacts from that cache
- nothing is uploaded to GitHub or Maven Central
- this is the fastest way to test one local project against another local project

Typical local flow:

1. In `QRForge4J`, run `./gradlew publishAllToMavenLocal`
2. In your Ktor app, add `mavenLocal()` to `repositories`
3. In your Ktor app, declare `implementation("io.github.willmortimer:qrgen-ktor:1.0.0")`
4. Run the Ktor app normally
5. If you make QRForge4J changes, rerun `publishAllToMavenLocal` so the local cache gets the updated artifact

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

QRForge4J now has build wiring for Maven Central publishing through the Sonatype Central Portal compatibility service, but it still requires Sonatype account setup and release secrets before GitHub Actions can actually publish there.

Required setup:

- a verified Sonatype Central namespace for `io.github.willmortimer`
- Central publishing credentials from the Sonatype Portal
- an ASCII-armored GPG private key for signing
- repository secrets in GitHub Actions:
  - `CENTRAL_PORTAL_USERNAME`
  - `CENTRAL_PORTAL_PASSWORD`
  - `SIGNING_KEY`
  - `SIGNING_PASSWORD`

Once that is set up, the release workflow can publish the same artifacts to Maven Central in addition to GitHub Packages.
