plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("maven-publish")
}

group = "io.github.qrgen"
version = "1.0.0"

dependencies {
    implementation(project(":qrgen-core"))
    implementation(project(":qrgen-svg"))
    implementation(project(":qrgen-png"))
    implementation(project(":qrgen-batch"))
    
    // Micronaut dependencies
    implementation("io.micronaut:micronaut-inject:3.10.3")
    implementation("io.micronaut:micronaut-http:3.10.3")
    implementation("io.micronaut:micronaut-http-server-netty:3.10.3")
    implementation("io.micronaut:micronaut-runtime:3.10.3")
    
    // Configuration
    implementation("io.micronaut:micronaut-context:3.10.3")
    
    // JSON support
    implementation("io.micronaut:micronaut-jackson-databind:3.10.3")
    
    // Coroutines support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.3")
    
    // Annotation processing
    kapt("io.micronaut:micronaut-inject-java:3.10.3")
    
    // Testing
    testImplementation("io.micronaut.test:micronaut-test-junit5:4.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
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