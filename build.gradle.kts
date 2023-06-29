plugins {
    `maven-publish`

    alias(libs.plugins.quilt.loom)
//    alias(libs.plugins.fabric.loom)

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    altoclef.repositories
}

version = "1.19.4-beta3"
group = "gay.solonovamax.altoclef"

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
}

dependencies {
    minecraft(libs.minecraft)
    // mappings(variantOf(libs.quilt.mappings) { classifier("intermediary-v2") })
    mappings(variantOf(libs.yarn.mappings) { classifier("v2") })
    // mappings(loom.layered {
    //     // officialMojangMappings()
    //     // mappings(variantOf(libs.quilt.mappings) { classifier("intermediary-v2") })
    //     mappings(variantOf(libs.yarn.mappings) { classifier("v2") })
    // })

    modImplementation(libs.fabric.loader)

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.language.kotlin)

    // Cloud command processor
    modImplementation(libs.bundles.cloud) {
        include(this)
        exclude(group = "net.fabricmc.fabric-api")
    }

    // Silk
    modImplementation(libs.bundles.silk) {
        include(this)
    }

    // Adventure
    modImplementation(libs.bundles.adventure) {
        include(this)
    }

    // Jackson
    implementation(libs.bundles.jackson) {
        include(this)
    }

    // SLF4K (SLF4J wrapper)
    implementation(libs.slf4k) {
        include(this)
    }

    // Runtime mods for performance
//    modLocalRuntime(libs.bundles.mod.runtime)

    modRuntimeOnly(libs.bundles.mod.runtime)

    // Thank you georgeagostino for fixing my garbage
    if (project.hasProperty("altoclef.development")) {
        // Must run build from baritone-plus once
        modImplementation("baritone-api-fabric:baritone-unoptimized-fabric-1.19.4-beta1")
        include("baritone-api-fabric:baritone-unoptimized-fabric-1.19.4-beta1")
    } else {
        modImplementation("cabaletta:baritone-unoptimized-fabric:1.19.4-beta1")
        include("cabaletta:baritone-unoptimized-fabric:1.19.4-beta1")
    }
}

// run configuration to support mixin hotswapping
afterEvaluate {
    loom {
        runConfigs.configureEach {
            property("fabric.development=true")
            property("mixin.hotSwap")

            val mixinJarFile = configurations.compileClasspath.get().files {
                it.group == "net.fabricmc" && it.name == "sponge-mixin"
            }.first()
            vmArg("-javaagent:$mixinJarFile")

            ideConfigGenerated(true)
        }
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<Jar> {
        from("LICENSE") {
            rename { "${it}_${rootProject.name}" }
        }
    }
}
