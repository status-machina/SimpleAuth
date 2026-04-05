plugins {
    java
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    // 26.1 is UNOBFUSCATED - NO mappings line!
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")

    // Use implementation (not modImplementation) for 26.1
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    include(implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")!!)

    // Security & Database
    include(implementation("org.mindrot:jbcrypt:0.4")!!)
    include(implementation("org.xerial:sqlite-jdbc:3.45.1.0")!!)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    withType<JavaCompile> {
        options.release = 25
    }

    // Use jar (not remapJar) for 26.1
    jar {
        from("LICENSE") {
            rename { "${it}_${project.property("archives_base_name")}" }
        }
    }
}
