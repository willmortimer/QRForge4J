plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `maven-publish`
}

group = "io.github.qrgen"
version = "1.0.0"

dependencies {
    api(project(":qrgen-core"))
    api(project(":qrgen-svg"))
    
    // Apache Batik for SVG to PNG conversion
    api("org.apache.xmlgraphics:batik-transcoder:1.17")
    api("org.apache.xmlgraphics:batik-codec:1.17")
    api("org.apache.xmlgraphics:batik-rasterizer:1.17")
    
    implementation(kotlin("stdlib"))
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation(project(":qrgen-test"))
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
                name.set("QRGen PNG Renderer")
                description.set("High-quality PNG rendering using Apache Batik")
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