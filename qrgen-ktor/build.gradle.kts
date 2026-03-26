plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `maven-publish`
}

group = "io.github.willmortimer"
version = "1.0.0"

val ktorVersion = "3.3.1"

dependencies {
    api(project(":qrgen-core"))
    api(project(":qrgen-svg"))
    api(project(":qrgen-png"))
    api(project(":qrgen-pdf"))
    api(project(":qrgen-dsl"))

    api("io.ktor:ktor-server-core:$ktorVersion")
    api("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("QRGen Ktor Integration")
                description.set("Ktor routing and service helpers for QRGen")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
