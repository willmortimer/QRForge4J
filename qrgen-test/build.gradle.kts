plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `maven-publish`
}

group = "io.github.willmortimer"
version = "1.0.0"

dependencies {
    api(project(":qrgen-core"))
    api(project(":qrgen-svg"))
    api(project(":qrgen-dsl"))
    
    // ZXing for QR code scanning/verification
    api("com.google.zxing:core:3.5.2")
    api("com.google.zxing:javase:3.5.2")
    
    implementation(kotlin("stdlib"))
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("QRGen Testing Utilities")
                description.set("ZXing-based testing and verification utilities for QR codes")
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