import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.dokka") version "1.9.10"
    id("maven-publish")
    signing
}

fun deriveReleaseVersion(): String {
    val explicit = System.getenv("RELEASE_VERSION")
        ?: (findProperty("releaseVersion") as String?)
    if (!explicit.isNullOrBlank()) return explicit.removePrefix("v")

    val githubRef = System.getenv("GITHUB_REF_NAME")
    if (!githubRef.isNullOrBlank() && githubRef.startsWith("v")) {
        return githubRef.removePrefix("v")
    }

    return runCatching {
        val process = ProcessBuilder("git", "describe", "--tags", "--exact-match")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        if (process.waitFor() == 0 && output.isNotBlank()) output.removePrefix("v") else "1.0.0-SNAPSHOT"
    }.getOrDefault("1.0.0-SNAPSHOT")
}

group = "io.github.willmortimer"
version = deriveReleaseVersion()

val projectUrl = "https://github.com/willmortimer/QRForge4J"
val githubPackagesUrl = "https://maven.pkg.github.com/willmortimer/QRForge4J"
val sonatypeStagingApiBase = "https://ossrh-staging-api.central.sonatype.com"
val sonatypeStagingDeployUrl = "$sonatypeStagingApiBase/service/local/staging/deploy/maven2/"
val centralNamespace = providers.environmentVariable("CENTRAL_NAMESPACE")
    .orElse(providers.gradleProperty("central.namespace"))
    .orElse("io.github.willmortimer")
val centralUsername = providers.environmentVariable("CENTRAL_PORTAL_USERNAME")
    .orElse(providers.gradleProperty("central.username"))
val centralPassword = providers.environmentVariable("CENTRAL_PORTAL_PASSWORD")
    .orElse(providers.gradleProperty("central.password"))
val signingKey = providers.environmentVariable("SIGNING_KEY")
    .orElse(providers.gradleProperty("signingInMemoryKey"))
val signingPassword = providers.environmentVariable("SIGNING_PASSWORD")
    .orElse(providers.gradleProperty("signingInMemoryKeyPassword"))
val signingEnabled = providers.environmentVariable("ENABLE_SIGNING")
    .orElse(providers.gradleProperty("enableSigning"))
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("io.github.willmortimer:qrgen-core:${version}")
    implementation("io.github.willmortimer:qrgen-svg:${version}")
    implementation("io.github.willmortimer:qrgen-png:${version}")
    
    implementation(gradleApi())
    implementation(localGroovy())
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("qrgen") {
            id = "io.github.willmortimer.qrgen"
            implementationClass = "io.github.qrgen.gradle.QrGenPlugin"
            displayName = "QRGen Gradle Plugin"
            description = "Gradle plugin for generating QR codes at build time"
        }
    }
}

// Configure publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("QRGen Gradle Plugin")
                description.set("Gradle plugin for QR code generation")
                url.set("https://github.com/willmortimer/QRForge4J")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("willmortimer")
                        name.set("Will Mortimer")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/willmortimer/QRForge4J.git")
                    developerConnection.set("scm:git:ssh://github.com/willmortimer/QRForge4J.git")
                    url.set("https://github.com/willmortimer/QRForge4J")
                }
            }
        }
    }
    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri(githubPackagesUrl)
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                    ?: providers.gradleProperty("gpr.user").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
                    ?: providers.gradleProperty("gpr.key").orNull
            }
        }
        if (centralUsername.isPresent && centralPassword.isPresent) {
            maven {
                name = "SonatypeCentral"
                url = uri(sonatypeStagingDeployUrl)
                credentials {
                    username = centralUsername.get()
                    password = centralPassword.get()
                }
            }
        }
    }
}

signing {
    if (signingEnabled.get() && signingKey.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
        sign(publishing.publications)
    }
}

tasks.register("publishAllToSonatypeCentral") {
    dependsOn(tasks.matching { it.name.endsWith("ToSonatypeCentralRepository") })
}

tasks.register("releaseToCentralPortal") {
    dependsOn("publishAllToSonatypeCentral")

    doLast {
        val username = centralUsername.orNull ?: error("Missing CENTRAL_PORTAL_USERNAME or central.username")
        val password = centralPassword.orNull ?: error("Missing CENTRAL_PORTAL_PASSWORD or central.password")
        val namespace = centralNamespace.get()
        val auth = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$sonatypeStagingApiBase/manual/upload/defaultRepository/$namespace?publishing_type=automatic"))
            .header("Authorization", "Bearer $auth")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "Central Portal upload failed (${response.statusCode()}): ${response.body()}"
        }
        logger.lifecycle("Released Gradle plugin artifacts to Central Portal for namespace {}", namespace)
    }
}
