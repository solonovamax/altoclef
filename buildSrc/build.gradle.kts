plugins {
    `kotlin-dsl`
    // alias(libs.plugins.kotlin.jvm)
    // kotlin("jvm") apply false
}

repositories {
    //maven("https://maven.solo-studios.ca/releases/")

    mavenCentral()
    // for kotlin-dsl plugin
    gradlePluginPortal()
}

dependencies {
    // implementation("")
}
