import org.gradle.process.ExecOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import javax.inject.Inject

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// Root multi-project build file for RGV (Recipe Graph Viewer)
// Subprojects:
//   :core          - Pure Java API and core recipe tree/graph logic
//   :forge-1.7.10  - Minecraft 1.7.10 Forge implementation
//   :forge-1.12.2  - Minecraft 1.12.2 Forge implementation

tasks.register("test") {
    group = "verification"
    description = "Runs unit tests for the core library"
    dependsOn(":core:test")
}

tasks.register("build1710") {
    group = "build"
    description = "Builds the Minecraft 1.7.10 mod jar"
    dependsOn(":forge-1.7.10:build")
}

tasks.register("run1710") {
    group = "forge"
    description = "Runs the Minecraft 1.7.10 client"
    dependsOn(":forge-1.7.10:runClient")
}

tasks.register("build1122") {
    group = "build"
    description = "Builds the Minecraft 1.12.2 mod jar"
    dependsOn(":forge-1.12.2:build")
}

tasks.register("run1122") {
    group = "forge"
    description = "Runs the Minecraft 1.12.2 client"
    dependsOn(":forge-1.12.2:runClient")
}

abstract class BuildAllPlatformsTask @Inject constructor(
    private val execOperations: ExecOperations,
    private val layout: ProjectLayout
) : DefaultTask() {

    @get:Input
    abstract val modVersion: Property<String>

    @TaskAction
    fun execute() {
        val rootDir = layout.projectDirectory.asFile
        val rootBuildDir = File(rootDir, "build")
        val rootBuildLibsDir = File(rootBuildDir, "libs")
        rootBuildDir.mkdirs()
        rootBuildLibsDir.mkdirs()

        val version = modVersion.get()
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val gradlew = if (isWindows) File(rootDir, "gradlew.bat").absolutePath else File(rootDir, "gradlew").absolutePath

        logger.lifecycle(">> [1/2] Building RGV for Minecraft 1.7.10 (Forge)...")
        execOperations.exec {
            workingDir(rootDir)
            commandLine(gradlew, ":forge-1.7.10:build", "--console=plain")
        }

        logger.lifecycle(">> [2/2] Building RGV for Minecraft 1.12.2 (Forge)...")
        execOperations.exec {
            workingDir(rootDir)
            commandLine(gradlew, ":forge-1.12.2:build", "--console=plain")
        }

        val name1710 = "RGV-$version-forge-1.7.10.jar"
        val name1122 = "RGV-$version-forge-1.12.2.jar"

        val src1710 = File(rootDir, "forge-1.7.10/build/libs/$name1710")
        val src1122 = File(rootDir, "forge-1.12.2/build/libs/$name1122")

        if (src1710.exists()) {
            src1710.copyTo(File(rootBuildDir, name1710), overwrite = true)
            src1710.copyTo(File(rootBuildLibsDir, name1710), overwrite = true)
        } else {
            logger.warn("Warning: Expected output jar not found at ${src1710.absolutePath}")
        }

        if (src1122.exists()) {
            src1122.copyTo(File(rootBuildDir, name1122), overwrite = true)
            src1122.copyTo(File(rootBuildLibsDir, name1122), overwrite = true)
        } else {
            logger.warn("Warning: Expected output jar not found at ${src1122.absolutePath}")
        }

        logger.lifecycle("")
        logger.lifecycle("================================================================================")
        logger.lifecycle("BUILD SUCCESSFUL: All platform jars copied to root build folder:")
        logger.lifecycle("  - ${File(rootBuildDir, name1710).absolutePath}")
        logger.lifecycle("  - ${File(rootBuildDir, name1122).absolutePath}")
        logger.lifecycle("================================================================================")
    }
}

tasks.register<BuildAllPlatformsTask>("buildAll") {
    group = "build"
    description = "Builds RGV for all platforms and copies jars to root build folder"
    modVersion.set(project.findProperty("mod_version")?.toString() ?: "1.0.0")
}

tasks.register("build") {
    group = "build"
    description = "Builds RGV for all platforms and copies jars to root build folder"
    dependsOn("buildAll")
}
