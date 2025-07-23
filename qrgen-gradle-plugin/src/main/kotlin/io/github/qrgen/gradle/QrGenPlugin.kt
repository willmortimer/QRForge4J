package io.github.qrgen.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * QRGen Gradle plugin for build-time QR code generation
 */
class QrGenPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        // Register the extension
        val extension = project.extensions.create("qrgen", QrGenExtension::class.java)
        
        // Register the task type
        project.tasks.register("generateQrCodes", QrGenTask::class.java) { task ->
            task.group = "qrgen"
            task.description = "Generate QR codes defined in the qrgen configuration"
            task.qrConfigs.set(extension.qrCodes)
            task.outputDir.set(project.layout.buildDirectory.dir("generated/qr"))
        }
        
        // Register task for single QR generation
        project.tasks.register("generateQrCode", QrGenSingleTask::class.java) { task ->
            task.group = "qrgen"
            task.description = "Generate a single QR code with command line parameters"
            task.outputDir.set(project.layout.buildDirectory.dir("generated/qr"))
        }
        
        // Auto-wire to build process if configured
        project.afterEvaluate {
            if (extension.generateOnBuild.get()) {
                val processResources = project.tasks.findByName("processResources")
                processResources?.dependsOn("generateQrCodes")
            }
        }
    }
} 