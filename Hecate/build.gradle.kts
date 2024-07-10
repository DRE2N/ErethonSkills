import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import java.io.FileInputStream

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://ci.emc.gs/nexus/content/groups/aikar/")
    maven("https://repo.aikar.co/content/groups/aikar")
    maven("https://repo.md-5.net/content/repositories/releases/")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://erethon.de/repo")
}
plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("io.github.goooler.shadow") version "8.1.5"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

group = "de.erethon"
version = "1.0-SNAPSHOT"
description = "Spell plugin for Erethon"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val papyrusVersion = "1.21-R0.1-SNAPSHOT"

dependencies {
    paperweight.devBundle("de.erethon.papyrus", papyrusVersion) { isChanging = true}
    //paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT") { isChanging = true }
    //compileOnly("de.erethon.papyrus:papyrus-api:1.19")
    implementation("de.erethon:bedrock:1.3.0") { isTransitive = false }
    implementation(project(":SpellbookSpells"))
    //compileOnly("com.comphenix.protocol:ProtocolLib:4.8.0")
}


tasks {
    paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
    runServer {
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir, "server.jar");
        uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-$papyrusVersion-reobf.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        dependencies {
            include(dependency("de.erethon:bedrock:1.3.0"))
            include(project(":SpellbookSpells"))
            include(dependency("com.elmakers.mine.bukkit:EffectLib:9.4"))
        }
        relocate("de.erethon.bedrock", "de.erethon.hecate.bedrock")
        relocate("com.elmakers.mine.bukkit", "de.erethon.hecate.effectlib")
    }
    jar {
        manifest {
            attributes(
                "paperweight-mappings-namespace" to "mojang"
            )
        }
    }
    bukkit {
        load = BukkitPluginDescription.PluginLoadOrder.STARTUP
        main = "de.erethon.hecate.Hecate"
        apiVersion = "1.18"
        authors = listOf("Malfrador", "Fyreum")
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
