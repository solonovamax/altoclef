package gay.solonovamax.altoclef.command

import adris.altoclef.tasks.movement.IdleTask
import adris.altoclef.util.helpers.WorldHelper
import ca.solostudios.adventure.kotlin.dsl.text
import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import gay.solonovamax.altoclef.AltoClef
import gay.solonovamax.altoclef.command.source.ClientCommand
import net.minecraft.client.MinecraftClient

@CommandMethod("altoclef")
class UtilCommands {
    @CommandMethod("gamma [gamma]")
    @CommandDescription("Update the game's gamma (brightness) value")
    fun gammaCommand(
        command: ClientCommand,
        @Argument("gamma", defaultValue = "0.5", description = "The new gamma value")
        gamma: Double,
    ) {
        MinecraftClient.getInstance().options.gamma.value = gamma
    }

    @CommandMethod("coords")
    @CommandDescription("Get the bot's current coordinates")
    fun coordsCommand(command: ClientCommand) {
        val message = text {
            content("Current coordinates: ${AltoClef.player?.blockPos?.toShortString()} (Current dimension: ${WorldHelper.getCurrentDimension()})")
        }
        command.sendMessage(message)
    }

    @CommandMethod("idle")
    @CommandDescription("Stand still")
    fun idleCommand(
        command: ClientCommand,
    ) {
        AltoClef.runUserTask(IdleTask())
    }
}
