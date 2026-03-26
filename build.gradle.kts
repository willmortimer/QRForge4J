import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm") version "2.3.10" apply false
    kotlin("kapt") version "2.3.10" apply false
    id("org.jetbrains.dokka") version "1.9.10"
}

// Configure Dokka for the entire project
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
                    url = uri("https://maven.pkg.github.com/willmortimer/QRForge4J")
                    credentials {
                        username = providers.environmentVariable("GITHUB_ACTOR").orNull
                            ?: providers.gradleProperty("gpr.user").orNull
                        password = providers.environmentVariable("GITHUB_TOKEN").orNull
                            ?: providers.gradleProperty("gpr.key").orNull
                    }
                }
            }
        }

        afterEvaluate {
            extensions.findByType(PublishingExtension::class.java)
                ?.publications
                ?.withType(MavenPublication::class.java)
                ?.configureEach {
                    artifactId = project.name
                    pom {
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
    }
}

tasks.register("publishAllToMavenLocal") {
    dependsOn(subprojects.mapNotNull { 
        it.tasks.findByName("publishToMavenLocal") 
    })
}

tasks.register("publishAll") {
    dependsOn(subprojects.mapNotNull { 
        it.tasks.findByName("publish") 
    })
}

tasks.register("verifyDocs") {
    dependsOn("dokkaHtmlMultiModule")
}

tasks.register("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
} 
