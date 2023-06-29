package gay.solonovamax.altoclef.command

import adris.altoclef.TaskCatalogue
import adris.altoclef.tasks.entity.HeroTask
import adris.altoclef.tasks.entity.KillPlayerTask
import adris.altoclef.ui.MessagePriority
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import gay.solonovamax.altoclef.AltoClef
import gay.solonovamax.altoclef.command.source.ClientCommand

@CommandMethod("altoclef")
class MiscCommands {
    @CommandMethod("punk")
    @CommandDescription("Punk 'em (attempts to kill a player)")
    fun punkCommand(
        command: ClientCommand,
        playerName: String, // TODO use a client-side version of SinglePlayerSelector
    ) {
        AltoClef.runUserTask(KillPlayerTask(playerName))
    }

    @CommandMethod("list")
    @CommandDescription("List all obtainable items")
    fun listCommand(
        command: ClientCommand,
    ) {
        AltoClef.log("#### LIST OF ALL OBTAINABLE ITEMS ####", MessagePriority.OPTIONAL)
        AltoClef.log(TaskCatalogue.resourceNames().joinToString { it.toString() }, MessagePriority.OPTIONAL)
        AltoClef.log("############# END LIST ###############", MessagePriority.OPTIONAL)
    }

    @CommandMethod("hero")
    @CommandDescription("Kill all hostile mobs")
    fun heroCommand(
        command: ClientCommand,
    ) {
        AltoClef.runUserTask(HeroTask())
    }
}
