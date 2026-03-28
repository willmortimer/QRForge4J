plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `maven-publish`
}

dependencies {
    api(project(":qrgen-core"))
    api(project(":qrgen-svg"))
    api(project(":qrgen-png"))
    api(project(":qrgen-pdf"))
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
                url.set("https://github.com/willmortimer/QRForge4J")
                
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

tasks.register<JavaExec>("runBenchmarks") {
    group = "verification"
    description = "Run QRGen benchmark scenarios and write reports"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.qrgen.test.QrBenchmarkRunnerKt")
}

tasks.register<JavaExec>("generateGoldens") {
    group = "verification"
    description = "Generate normalized SVG and raster smoke goldens under build/generated-goldens"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.qrgen.test.QrGoldenGeneratorKt")
}
