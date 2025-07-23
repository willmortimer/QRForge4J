plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `maven-publish`
}

group = "io.github.willmortimer"
version = "1.0.0"

val springBootVersion = "3.2.0"

dependencies {
    api(project(":qrgen-core"))
    api(project(":qrgen-svg"))
    api(project(":qrgen-png"))
    api(project(":qrgen-dsl"))
    api(project(":qrgen-batch"))
    
    // Spring Boot
    api("org.springframework.boot:spring-boot-starter:$springBootVersion")
    api("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    api("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
    
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation(project(":qrgen-test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("QRGen Spring Boot Starter")
                description.set("Spring Boot auto-configuration for QRGen library")
                url.set("https://github.com/yourusername/qr-generator")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
} 