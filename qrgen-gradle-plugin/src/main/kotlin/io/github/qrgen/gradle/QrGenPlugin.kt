package io.github.qrgen.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * QRGen Gradle plugin for build-time QR code generation
 */
class QrGenPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        // Register the extension
        val extension = project.extensions.create("qrgen", QrGenExtension::class.java)
        
        // Register the task type
        val generateQrCodes = project.tasks.register("generateQrCodes", QrGenTask::class.java)
        generateQrCodes.configure {
            group = "qrgen"
            description = "Generate QR codes defined in the qrgen configuration"
            qrConfigs.set(extension.qrCodes)
            outputDir.set(project.layout.buildDirectory.dir("generated/qr"))
        }
        
        // Register task for single QR generation
        val generateQrCode = project.tasks.register("generateQrCode", QrGenSingleTask::class.java)
        generateQrCode.configure {
            group = "qrgen"
            description = "Generate a single QR code with command line parameters"
            outputDir.set(project.layout.buildDirectory.dir("generated/qr"))
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
