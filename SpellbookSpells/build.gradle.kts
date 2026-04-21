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
    paperweight.devBundle("de.erethon.papyrus", "26.1.2-SNAPSHOT") { isChanging = true }
    implementation("com.elmakers.mine.bukkit:EffectLib:10.12-SNAPSHOT")
    implementation("de.erethon:Daedalus:1.4-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
        targetCompatibility = JavaVersion.VERSION_25.name
        sourceCompatibility = JavaVersion.VERSION_25.name
    }
}