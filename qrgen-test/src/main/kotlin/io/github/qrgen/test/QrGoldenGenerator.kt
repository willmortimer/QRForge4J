package io.github.qrgen.test

import java.io.File

fun main() {
    val outputDir = File("build/generated-goldens")
    outputDir.mkdirs()
    QrGoldenSupport.writeGoldens(outputDir)
    println("Generated goldens in ${outputDir.absolutePath}")
}
