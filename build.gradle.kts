import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

plugins {
    kotlin("jvm") version "2.3.10" apply false
    kotlin("kapt") version "2.3.10" apply false
    id("org.jetbrains.dokka") version "1.9.10"
    signing
}

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
val centralPublishingEnabled = providers.provider {
    centralUsername.isPresent && centralPassword.isPresent
}

tasks.dokkaHtmlMultiModule.configure {
    moduleName.set("QRGen")
    outputDirectory.set(rootDir.resolve("docs/api"))
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """{"footerMessage":"QRForge4J API Documentation"}"""
        )
    )
}

allprojects {
    group = "io.github.willmortimer"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.apply("signing")

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                freeCompilerArgs.addAll(
                    "-Xjsr305=strict",
                    "-opt-in=kotlin.RequiresOptIn",
                    "-Xannotation-default-target=param-property"
                )
            }
        }
    }

    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
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
                if (centralPublishingEnabled.get()) {
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

        extensions.configure<SigningExtension> {
            if (signingKey.isPresent) {
                useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
            }
        }

        afterEvaluate {
            val publishing = extensions.findByType(PublishingExtension::class.java) ?: return@afterEvaluate
            publishing.publications.withType(MavenPublication::class.java).configureEach {
                artifactId = project.name
                pom {
                    name.convention(project.name)
                    description.convention(
                        providers.provider { project.description ?: "QRForge4J module ${project.name}" }
                    )
                    url.set(projectUrl)
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
                        url.set(projectUrl)
                    }
                }
            }

            if (signingKey.isPresent) {
                extensions.findByType(SigningExtension::class.java)?.sign(publishing.publications)
            }
        }
    }
}

tasks.register("publishAllToMavenLocal") {
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("publishToMavenLocal") })
}

tasks.register("publishAll") {
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("publish") })
}

tasks.register("publishAllToSonatypeCentral") {
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("publishAllPublicationsToSonatypeCentralRepository") })
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
        logger.lifecycle("Released artifacts to Central Portal for namespace {}", namespace)
    }
}

tasks.register("verifyDocs") {
    dependsOn("dokkaHtmlMultiModule")
}

tasks.named("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
}
