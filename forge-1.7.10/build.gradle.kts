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

dependencies {
    implementation(project(":core"))
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

