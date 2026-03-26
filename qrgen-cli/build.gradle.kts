plugins {
    kotlin("jvm")
    application
}

group = "io.github.willmortimer"
version = "1.0.0"

dependencies {
    implementation(project(":qrgen-core"))
    implementation(project(":qrgen-dsl"))
    implementation(project(":qrgen-png"))
    implementation(project(":qrgen-pdf"))
    implementation(kotlin("stdlib"))
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("io.github.qrgen.cli.QrGenCliKt")
}

tasks.test {
    useJUnitPlatform()
} 
