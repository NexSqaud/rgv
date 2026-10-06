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
    mcVersion.set("1.12.2")
    username.set("Developer")
}

repositories {
    mavenCentral()
    maven {
        name = "Protonpack / DMod maven"
        url = uri("https://dvs1.progwml6.com/files/maven/")
    }
    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
    }
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
    ?: !gradle.startParameter.taskNames.any {
        it.contains("WithoutJei", ignoreCase = true) ||
        it.contains("NoJei", ignoreCase = true)
    }

dependencies {
    implementation(project(":core"))
    compileOnly("mezz.jei:jei_1.12.2:4.16.1.302:api")
    if (withJei) {
        runtimeOnly("mezz.jei:jei_1.12.2:4.16.1.302")
    }
}

val modVersion = project.findProperty("mod_version")?.toString() ?: "1.0.0"

base {
    archivesName.set("RGV-$modVersion-forge-1.12.2")
}

tasks.jar {
    from(project(":core").sourceSets["main"].output)
}

tasks.named("test") {
    enabled = false
}

tasks.register("runClientWithJei") {
    group = "forge"
    description = "Runs the Minecraft 1.12.2 client with JEI recipe viewer"
    dependsOn("runClient")
}

tasks.register("runClientWithoutJei") {
    group = "forge"
    description = "Runs the Minecraft 1.12.2 client without JEI recipe viewer"
    dependsOn("runClient")
}

