import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

repositories {
    maven("https://erethon.de/repo")
    maven("https://repo.dmulloy2.net/repository/public/")
}
plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.3.6-SNAPSHOT"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

group = "de.erethon.hecate"
version = "1.0."
description = "Spell plugin for Erethon"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    paperDevBundle("1.18.2-R0.1-SNAPSHOT")
    implementation("de.erethon:bedrock:1.2.0") { isTransitive = false }
    implementation(project(":Spellbook"))
    //compileOnly("com.comphenix.protocol:ProtocolLib:4.8.0")
}


tasks {
    assemble {
        dependsOn(reobfJar)
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        dependencies {
            include(dependency("de.erethon:bedrock:1.2.0"))
            include(project(":Spellbook"))
        }
        relocate("de.erethon.bedrock", "de.erethon.hecate.bedrock")
    }
    bukkit {
        load = BukkitPluginDescription.PluginLoadOrder.STARTUP
        main = "de.erethon.hecate.Hecate"
        apiVersion = "1.18"
        authors = listOf("Malfrador")
        commands {
            register("hecate") {
                description = "Main command for Hecate"
                aliases = listOf("h", "hc")
                permission = "hecate.cmd"
                usage = "/hecate help"
            }
        }
    }
}
