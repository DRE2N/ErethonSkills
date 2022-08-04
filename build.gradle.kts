plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.3.8-SNAPSHOT"
}

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
tasks {
    assemble {
        dependsOn("Spellbook:assemble")
        dependsOn("Hecate:assemble")
    }


}



dependencies {
    paperweightDevBundle("de.erethon.papyrus", "1.19.1-R0.1-SNAPSHOT")
}