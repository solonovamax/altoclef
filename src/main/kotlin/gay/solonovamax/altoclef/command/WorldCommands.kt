package gay.solonovamax.altoclef.command

import adris.altoclef.tasks.construction.CoverWithBlocksTask
import adris.altoclef.tasks.construction.CoverWithSandTask
import adris.altoclef.tasks.movement.GoToStrongholdPortalTask
import adris.altoclef.tasks.movement.LocateDesertTempleTask
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import gay.solonovamax.altoclef.AltoClef
import gay.solonovamax.altoclef.command.source.ClientCommand

@CommandMethod("altoclef")
class WorldCommands {
    @CommandMethod("locate")
    @CommandDescription("Locate the given structure")
    fun locateStructureCommand(
        command: ClientCommand,
        structure: Structure
    ) {
        when (structure) {
            Structure.STRONGHOLD -> AltoClef.runUserTask(GoToStrongholdPortalTask(1))
            Structure.DESERT_TEMPLE -> AltoClef.runUserTask(LocateDesertTempleTask())
        }
    }

    @CommandMethod("cover blocks")
    @CommandDescription("Cover nether lava with blocks")
    fun coverWithBlocksCommand(command: ClientCommand) {
        AltoClef.runUserTask(CoverWithBlocksTask())
    }

    @CommandMethod("cover sand")
    @CommandDescription("Cover nether lava with sand")
    fun coverWithSandCommand(command: ClientCommand) {
        AltoClef.runUserTask(CoverWithSandTask())
    }

    enum class Structure {
        DESERT_TEMPLE,
        STRONGHOLD
    }
}
