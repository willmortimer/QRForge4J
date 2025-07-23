plugins {
    kotlin("jvm")
    application
}

group = "io.github.qrgen"
version = "1.0.0"

dependencies {
    implementation(project(":qrgen-dsl"))
    implementation(kotlin("stdlib"))
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("io.github.qrgen.cli.QrGenCliKt")
}

tasks.test {
    useJUnitPlatform()
} 