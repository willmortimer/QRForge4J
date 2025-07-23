plugins {
    `kotlin-dsl`
    id("maven-publish")
}

group = "io.github.qrgen"
version = "1.0.0"

dependencies {
    implementation(project(":qrgen-core"))
    implementation(project(":qrgen-svg"))
    implementation(project(":qrgen-png"))
    
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
            id = "io.github.qrgen.gradle-plugin"
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
                url.set("https://github.com/qrgen-kotlin/qrgen")
                
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
                        email.set("team@qrgen.io")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/qrgen-kotlin/qrgen.git")
                    developerConnection.set("scm:git:ssh://github.com:qrgen-kotlin/qrgen.git")
                    url.set("https://github.com/qrgen-kotlin/qrgen/tree/main")
                }
            }
        }
    }
} 