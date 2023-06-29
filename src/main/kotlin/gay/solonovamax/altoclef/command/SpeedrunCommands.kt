package gay.solonovamax.altoclef.command

import adris.altoclef.tasks.speedrun.BeatMinecraft2Task
import adris.altoclef.tasks.speedrun.MarvionBeatMinecraftTask
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import gay.solonovamax.altoclef.AltoClef
import gay.solonovamax.altoclef.command.source.ClientCommand

@CommandMethod("altoclef")
class SpeedrunCommands {
    @CommandMethod("speedrun marvion")
    @CommandDescription("Beats the game (Marvion version).")
    fun marvionSpeedrunCommand(
        command: ClientCommand,
    ) {
        AltoClef.runUserTask(MarvionBeatMinecraftTask())
    }

    @CommandMethod("speedrun default")
    @CommandDescription("Beats the game")
    fun defaultSpeedrunCommand(
        clientCommand: ClientCommand,
    ) {
        AltoClef.runUserTask(BeatMinecraft2Task())
    }
}
