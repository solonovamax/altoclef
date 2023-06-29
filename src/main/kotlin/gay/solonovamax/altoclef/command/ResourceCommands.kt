package gay.solonovamax.altoclef.command

import adris.altoclef.tasks.resources.CollectMeatTask
import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import gay.solonovamax.altoclef.AltoClef
import gay.solonovamax.altoclef.command.source.ClientCommand

@CommandMethod("altoclef")
class ResourceCommands {
    @CommandMethod("meat <count>")
    @CommandDescription("Collects a certain amount of meat")
    fun meatCommand(
        command: ClientCommand,
        @Argument("count", defaultValue = "16")
        count: Int,
    ) {
        AltoClef.runUserTask(
            CollectMeatTask(count.toDouble())
        )
    }
}
