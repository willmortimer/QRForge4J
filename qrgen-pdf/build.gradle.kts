plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `maven-publish`
}

group = "io.github.willmortimer"
version = "1.0.0"

dependencies {
    api(project(":qrgen-core"))
    api(project(":qrgen-png"))
    api("org.apache.pdfbox:pdfbox:3.0.3")

    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
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
                name.set("QRGen PDF Renderer")
                description.set("PDF output for QRGen")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
