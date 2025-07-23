plugins {
    kotlin("jvm") version "2.2.0" apply false
    id("org.jetbrains.dokka") version "1.9.10"
}

// Configure Dokka for the entire project
tasks.dokkaHtmlMultiModule.configure {
    moduleName.set("QRGen")
    outputDirectory.set(rootDir.resolve("docs/api"))
    
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """
                {
                    "customAssets": ["${rootDir}/docs/assets/logo.png"],
                    "customStyleSheets": ["${rootDir}/docs/assets/custom.css"],
                    "footerMessage": "© 2024 QRGen Library - Advanced QR Code Generation for JVM"
                }
            """.trimIndent()
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
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-opt-in=kotlin.RequiresOptIn"
            )
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

tasks.register("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
} 