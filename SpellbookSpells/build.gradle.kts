plugins {
    id("java")
    id("io.papermc.paperweight.userdev")

}

group = "de.erethon"

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
    maven("https://maven.elmakers.com/repository/")
    maven("https://repo.erethon.de/snapshots")
}

configurations.all {
    /*resolutionStrategy {
        resolutionStrategy.cacheChangingModulesFor(60, "seconds") // Force-redownload Papyrus
    }*/
}

dependencies {
    paperweight.devBundle("de.erethon.papyrus", "1.21.9-R0.1-SNAPSHOT") { isChanging = true }
    implementation("com.elmakers.mine.bukkit:EffectLib:10.11-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
        targetCompatibility = JavaVersion.VERSION_21.name
        sourceCompatibility = JavaVersion.VERSION_21.name
    }
}