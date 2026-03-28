plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":qrgen-core"))
    implementation(project(":qrgen-dsl"))
    implementation(project(":qrgen-png"))
    implementation(project(":qrgen-pdf"))
    implementation(kotlin("stdlib"))
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

application {
    mainClass.set("io.github.qrgen.cli.QrGenCliKt")
}

tasks.test {
    useJUnitPlatform()
} 
