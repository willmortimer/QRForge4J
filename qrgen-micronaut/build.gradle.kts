plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka")
    id("maven-publish")
}

group = "io.github.willmortimer"
version = "1.0.0"

dependencies {
    api(project(":qrgen-core"))
    api(project(":qrgen-svg"))
    api(project(":qrgen-png"))
    api(project(":qrgen-pdf"))
    api(project(":qrgen-batch"))
    
    // Micronaut dependencies
    val micronautVersion = "3.10.3"
    implementation("io.micronaut:micronaut-inject:$micronautVersion")
    implementation("io.micronaut:micronaut-http:$micronautVersion")
    implementation("io.micronaut:micronaut-http-server-netty:$micronautVersion")
    implementation("io.micronaut:micronaut-runtime:$micronautVersion")
    
    // Configuration
    implementation("io.micronaut:micronaut-context:$micronautVersion")
    
    // JSON support
    implementation("io.micronaut:micronaut-jackson-databind:$micronautVersion")
    
    // Coroutines support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Annotation processing
    kapt("io.micronaut:micronaut-inject-java:$micronautVersion")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.test {
    useJUnitPlatform()
}

// Configure publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("QRGen Micronaut Integration")
                description.set("Micronaut integration for QRGen library")
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
}
