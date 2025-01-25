plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"

}

group = "de.erethon"

version = "1.0-SNAPSHOT"

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
    maven("https://repo.erethon.de/releases")
    maven("https://repo.erethon.de/snapshots")
}

dependencies {
    paperweight.devBundle("de.erethon.papyrus", "1.21.4-R0.1-SNAPSHOT") { isChanging = true }
    implementation("com.elmakers.mine.bukkit:EffectLib:10.3")
    compileOnly("de.erethon:bedrock:1.4.0")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
        targetCompatibility = JavaVersion.VERSION_21.name
        sourceCompatibility = JavaVersion.VERSION_21.name
    }
}