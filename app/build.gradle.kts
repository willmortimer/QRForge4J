/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/8.1.1/userguide/building_java_projects.html
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "2.2.0"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // This dependency is used by the application.
    implementation(kotlin("stdlib"))
    implementation("com.google.guava:guava:31.1-jre")
    implementation("io.nayuki:qrcodegen:1.8.0")
}

testing {
    suites {
        // Configure the built-in test suite
        @Suppress("unused")
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest("2.2.0")
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    // Define the main class for the application.
    mainClass.set("qrcli.AppKt")
}
