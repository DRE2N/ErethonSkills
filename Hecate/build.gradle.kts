import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

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
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper") version "3.0.2"// Adds runServer and runMojangMappedServer tasks for testing
    id("com.gradleup.shadow") version "9.3.1"
    id("de.eldoria.plugin-yml.paper") version "0.7.1"
}

configurations.all {
    /*resolutionStrategy {
        resolutionStrategy.cacheChangingModulesFor(60, "seconds") // Force-redownload Papyrus
    }*/
}

group = "de.erethon"
version = "1.3-SNAPSHOT"
description = "Spell plugin for Erethon"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

val papyrusVersion = "26.3"

dependencies {
    paperweight.devBundle("de.erethon.papyrus", "$papyrusVersion-SNAPSHOT") { isChanging = true }
    implementation(project(":SpellbookSpells"))
    implementation("de.erethon.rhea:Rhea:0.1.0-SNAPSHOT")
    compileOnly("de.erethon.tyche:Tyche:1.0-SNAPSHOT")
    compileOnly("de.erethon.aergia:Aergia:1.0.1")
}

tasks {
    paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
    runServer {
        dependsOn("deployToSharedServer")
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir,  "server.jar");
        uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-$papyrusVersion.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
        runDirectory.set(file("C:\\Dev\\Erethon"))
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
        targetCompatibility = JavaVersion.VERSION_25.name
        sourceCompatibility = JavaVersion.VERSION_25.name
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        dependsOn("verifyDisciplineRhea")
        from(layout.buildDirectory.dir("rhea-discipline/templates")) { into("rhea/discipline") }
        from(layout.projectDirectory.file("src/main/rhea/assets/AlegreyaSans-Regular.ttf")) { into("rhea/discipline") }
    }

    shadowJar {
        dependencies {
            include(project(":SpellbookSpells"))
            include(dependency("de.erethon.rhea:Rhea:0.1.0-SNAPSHOT"))
            include(dependency("com.elmakers.mine.bukkit:EffectLib:10.12-SNAPSHOT"))
        }
        // Comment relocations out for hotswapping
        relocate("com.elmakers.mine.bukkit", "de.erethon.hecate.effectlib")
    }
    jar {
        manifest {
            attributes(
                "paperweight-mappings-namespace" to "mojang"
            )
        }
    }
}

val rheaCompilerDir = file("../../Rhea/compiler")
val disciplineRheaSource = layout.projectDirectory.dir("src/main/rhea")
val disciplineRheaOutput = layout.buildDirectory.dir("rhea-discipline")

val installDisciplineRheaCompiler by tasks.registering(Exec::class) {
    inputs.files(file("../../Rhea/compiler/package.json"), file("../../Rhea/compiler/package-lock.json"))
    outputs.dir(file("../../Rhea/compiler/node_modules"))
    workingDir = rheaCompilerDir
    commandLine("npm.cmd", "ci")
}

val installDisciplineRheaBrowser by tasks.registering(Exec::class) {
    dependsOn(installDisciplineRheaCompiler)
    workingDir = rheaCompilerDir
    commandLine("node", "node_modules/playwright/cli.js", "install", "chromium")
}

val compileDisciplineRhea by tasks.registering(Exec::class) {
    dependsOn(installDisciplineRheaBrowser)
    inputs.file(file("../../Rhea/compiler/compile.mjs"))
    inputs.dir(disciplineRheaSource)
    outputs.dir(disciplineRheaOutput)
    workingDir = rheaCompilerDir
    commandLine("node", "compile.mjs", "--source", disciplineRheaSource.asFile.absolutePath,
        "--output", disciplineRheaOutput.get().asFile.absolutePath)
}

val verifyDisciplineRhea by tasks.registering(Exec::class) {
    dependsOn(compileDisciplineRhea)
    workingDir = rheaCompilerDir
    commandLine("node", "verify.mjs", disciplineRheaOutput.get().asFile.absolutePath, "6")
}

val disciplineRheaPackFragment by tasks.registering(Zip::class) {
    dependsOn(compileDisciplineRhea)
    archiveFileName.set("hecate-discipline-ui-fragment.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(disciplineRheaOutput.map { it.dir("pack") })
    from(layout.projectDirectory.dir("src/main/pack"))
}

val deployDisciplineRheaPackFragment by tasks.registering(Copy::class) {
    group = "Erethon"
    dependsOn(disciplineRheaPackFragment)
    from(disciplineRheaPackFragment.flatMap { it.archiveFile })
    into("C:/Dev/Erethon/plugins/Hecate/pack-fragments")
}

val combatHudPackFragment by tasks.registering(Zip::class) {
    archiveFileName.set("hecate-combat-hud-fragment.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(layout.projectDirectory.dir("src/main/combat-hud-pack"))
}

val deployCombatHudPackFragment by tasks.registering(Copy::class) {
    group = "Erethon"
    dependsOn(combatHudPackFragment)
    from(combatHudPackFragment.flatMap { it.archiveFile })
    into("C:/Dev/Erethon/plugins/Hecate/pack-fragments")
}

// The shared run directory already receives Hecate from deployToSharedServer.
// Adding the run task's automatic plugin jar would register Hecate twice.
runPaper {
    disablePluginJarDetection()
}

paper {
    main = "de.erethon.hecate.Hecate"
    apiVersion = "1.21"
    authors = listOf("Malfrador", "Fyreum")
    hasOpenClassloader = true
    serverDependencies {
        register("Tyche") {
            required = false
            joinClasspath = true
        }
        register("Aergia") {
            required = false
            joinClasspath = true
        }
    }
}

tasks.register<Copy>("deployToSharedServer") {
    doNotTrackState("")
    group = "Erethon"
    description = "Used for deploying the plugin to the shared server. runServer will do this automatically." +
            "This task is only for manual deployment when running runServer from another plugin."
    dependsOn(tasks.shadowJar, deployDisciplineRheaPackFragment, deployCombatHudPackFragment)
    from(layout.buildDirectory.file("libs/Hecate-$version-all.jar"))
    into("C:\\Dev\\Erethon\\plugins")
}

publishing {
    repositories {
        maven {
            name = "erethon"
            url = uri("https://repo.erethon.de/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "de.erethon.hecate"
            version = "${project.version}"
            artifactId = "Hecate"
            artifact(tasks.shadowJar) {
                classifier = ""
            }
            pom {
                packaging = "jar"
            }
        }
    }
}

