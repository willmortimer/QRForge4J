plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `maven-publish`
}

dependencies {
    api("io.nayuki:qrcodegen:1.8.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
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
                name.set("QRGen Core")
                description.set("Core QR code generation and styling engine")
                url.set("https://github.com/willmortimer/QRForge4J")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("willmortimer")
                        name.set("Will Mortimer")
                    }
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
} 
