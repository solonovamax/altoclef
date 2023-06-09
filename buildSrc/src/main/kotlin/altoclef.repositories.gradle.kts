repositories {
    maven("https://maven.solo-studios.ca/releases/") {
        name = "Solo Studios"
    }

    mavenCentral()

    maven("https://maven.quiltmc.org/repository/release/") {
        name = "QuiltMC"
    }

    maven("https://maven.fabricmc.net/") {
        name = "FabricMC"
    }

    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content {
            includeGroup("maven.modrinth")
        }
    }

    maven("https://masa.dy.fi/maven") {
        name = "Masa Modding"
    }

    maven("https://maven.wispforest.io") {
        name = "Wisp Forest"
    }

    maven("https://marvionkirito.github.io/maven/") {
        name = "MarvionKiritoRepo"
    }

    flatDir {
        dir("../baritone/dist")
    }

    maven("https://oss.sonatype.org/content/repositories/snapshots") {
        mavenContent {
            snapshotsOnly()
        }
    }
}
