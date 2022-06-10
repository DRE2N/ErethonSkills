plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.3.6-SNAPSHOT"
}

repositories {
    mavenCentral()
}

dependencies {
    paperDevBundle("1.18.2-R0.1-SNAPSHOT")
}