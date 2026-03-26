plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.dokka") version "1.9.10"
    id("maven-publish")
}

group = "io.github.willmortimer"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("io.github.willmortimer:qrgen-core:1.0.0")
    implementation("io.github.willmortimer:qrgen-svg:1.0.0")
    implementation("io.github.willmortimer:qrgen-png:1.0.0")
    
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
                        id.set("qrgen-team")
                        name.set("QRGen Team")
                        email.set("contact@willmortimer.dev")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/willmortimer/QRForge4J.git")
                    developerConnection.set("scm:git:ssh://github.com/willmortimer/QRForge4J.git")
                    url.set("https://github.com/willmortimer/QRForge4J/tree/main")
                }
            }
        }
    }
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
