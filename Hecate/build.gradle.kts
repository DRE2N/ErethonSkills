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
    id("io.papermc.paperweight.userdev") version "1.3.8-SNAPSHOT"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

group = "de.erethon"
version = "1.0-SNAPSHOT"
description = "Spell plugin for Erethon"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

val papyrusVersion = "1.19.1-R0.1-SNAPSHOT"

dependencies {
    paperweightDevBundle("de.erethon.papyrus", papyrusVersion) { isChanging = true}
    //compileOnly("de.erethon.papyrus:papyrus-api:1.19")
    implementation("de.erethon:bedrock:1.2.4") { isTransitive = false }
    implementation(project(":SpellbookSpells"))
    //compileOnly("com.comphenix.protocol:ProtocolLib:4.8.0")
}


tasks {
    assemble {
        dependsOn(reobfJar)
    }

    runServer {
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir, "server.jar");
        //uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-$papyrusVersion-reobf.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
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
            include(dependency("de.erethon:bedrock:1.2.4"))
            include(project(":SpellbookSpells"))
            include(dependency("com.elmakers.mine.bukkit:EffectLib:9.4"))
        }
        relocate("de.erethon.bedrock", "de.erethon.hecate.bedrock")
        relocate("com.elmakers.mine.bukkit", "de.erethon.hecate.effectlib")
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
