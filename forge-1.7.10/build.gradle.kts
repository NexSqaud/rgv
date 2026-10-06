plugins {
    `java-library`
    id("com.gtnewhorizons.retrofuturagradle") version "2.0.5"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

minecraft {
    mcVersion.set("1.7.10")
    username.set("Developer")
}

repositories {
    mavenCentral()
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
    maven {
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
}

val withJei = project.findProperty("withJei")?.toString()?.toBoolean()
    ?: project.findProperty("withNei")?.toString()?.toBoolean()
    ?: !gradle.startParameter.taskNames.any {
        it.contains("WithoutJei", ignoreCase = true) ||
        it.contains("WithoutNei", ignoreCase = true) ||
        it.contains("NoJei", ignoreCase = true) ||
        it.contains("NoNei", ignoreCase = true)
    }

dependencies {
    implementation(project(":core"))
    if (withJei) {
        runtimeOnly("com.github.GTNewHorizons:CodeChickenCore:1.4.16:dev")
        runtimeOnly("com.github.GTNewHorizons:NotEnoughItems:2.7.52-GTNH:dev")
    }
}

val modVersion = project.findProperty("mod_version")?.toString() ?: "1.0.0"

base {
    archivesName.set("RGV-$modVersion-forge-1.7.10")
}

tasks.jar {
    from(project(":core").sourceSets["main"].output)
}

tasks.named("test") {
    enabled = false
}

tasks.register("runClientWithJei") {
    group = "forge"
    description = "Runs the Minecraft 1.7.10 client with NEI/JEI recipe viewer"
    dependsOn("runClient")
}

tasks.register("runClientWithNei") {
    group = "forge"
    description = "Runs the Minecraft 1.7.10 client with NEI recipe viewer (alias for runClientWithJei)"
    dependsOn("runClientWithJei")
}

tasks.register("runClientWithoutJei") {
    group = "forge"
    description = "Runs the Minecraft 1.7.10 client without NEI/JEI recipe viewer"
    dependsOn("runClient")
}

tasks.register("runClientWithoutNei") {
    group = "forge"
    description = "Runs the Minecraft 1.7.10 client without NEI recipe viewer (alias for runClientWithoutJei)"
    dependsOn("runClientWithoutJei")
}

