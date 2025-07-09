import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

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
    maven("https://repo.erethon.de/snapshots")
}
plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("io.github.goooler.shadow") version "8.1.5"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

configurations.all {
    resolutionStrategy {
        resolutionStrategy.cacheChangingModulesFor(60, "seconds") // Force-redownload Papyrus
    }
}

group = "de.erethon"
version = "1.1-SNAPSHOT"
description = "Spell plugin for Erethon"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val papyrusVersion = "1.21.7-R0.1-SNAPSHOT"

dependencies {
    paperweight.devBundle("de.erethon.papyrus", papyrusVersion) { isChanging = true }

    implementation("de.erethon:bedrock:1.5.7") { isTransitive = false }
    implementation(project(":SpellbookSpells"))
    compileOnly("de.erethon.aether:Aether:1.0.0-SNAPSHOT") // For correct nametags
    compileOnly("de.erethon.hephaestus:Hephaestus:1.0.3-SNAPSHOT")
}


tasks {
    paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
    runServer {
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir,  "server.jar");
        uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-$papyrusVersion-mojmap.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
        runDirectory.set(file("C:\\Dev\\Erethon"))
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
        targetCompatibility = JavaVersion.VERSION_21.name
        sourceCompatibility = JavaVersion.VERSION_21.name
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        dependencies {
            include(dependency("de.erethon:bedrock:1.5.7"))
            include(project(":SpellbookSpells"))
            include(dependency("com.elmakers.mine.bukkit:EffectLib:10.3"))
        }
        // Comment relocations out for hotswapping
        relocate("de.erethon.bedrock", "de.erethon.hecate.bedrock")
        relocate("com.elmakers.mine.bukkit", "de.erethon.hecate.effectlib")
    }
    jar {
        manifest {
            attributes(
                "paperweight-mappings-namespace" to "mojang"
            )
        }
        dependsOn(shadowJar)
    }
    bukkit {
        load = BukkitPluginDescription.PluginLoadOrder.STARTUP
        main = "de.erethon.hecate.Hecate"
        apiVersion = "1.21"
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

tasks.register<Copy>("deployToSharedServer") {
    doNotTrackState("")
    group = "Erethon"
    description = "Used for deploying the plugin to the shared server. runServer will do this automatically." +
            "This task is only for manual deployment when running runServer from another plugin."
    dependsOn(":jar")
    from(layout.buildDirectory.file("libs/Hecate-$version-all.jar"))
    into("C:\\Dev\\Erethon\\plugins")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "de.erethon.hecate"
            artifactId = "Hecate"
            version = "1.0-SNAPSHOT"
            artifactId = "Hecate"
            from(components["java"])
        }
    }
}

