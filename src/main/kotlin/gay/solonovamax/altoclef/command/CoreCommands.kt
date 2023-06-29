package gay.solonovamax.altoclef.command

import adris.altoclef.util.helpers.ConfigHelper
import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import cloud.commandframework.minecraft.extras.MinecraftHelp
import gay.solonovamax.altoclef.AltoClef
import gay.solonovamax.altoclef.command.source.ClientCommand
import net.kyori.adventure.extra.kotlin.text

@CommandMethod("altoclef")
class CoreCommands(
    private val help: MinecraftHelp<ClientCommand>
) {
    @CommandMethod("help [query]")
    @CommandDescription("Displays help menu")
    fun helpCommand(
        command: ClientCommand,
        @Greedy
        @Argument(value = "query", description = "The help query")
        query: String?,
    ) {
        help.queryCommands(query ?: "", command)
    }

    @CommandMethod("status")
    @CommandDescription("Display the status of the currently executing tasks")
    fun statusCommand(
        command: ClientCommand,
    ) {
        val tasks = AltoClef.userTaskChain.tasks

        val message = if (tasks.isNotEmpty()) {
            val currentTasks = tasks.joinToString(separator = "\n  - ", prefix = "\n  - ") {
                it.toString()
            }
            text {
                content(
                    """
                        Current task chain:
                        $currentTasks
                    """.trimIndent()
                )
            }
        } else {
            text {
                content("There are no currently executing tasks.")
            }
        }

        command.sendMessage(message)
    }

    @CommandMethod("stop")
    @CommandDescription("Stops the execution of the current task")
    fun stopCommand(
        command: ClientCommand,
    ) {
        AltoClef.userTaskChain.cancel()

        val message = text {
            content("Successfully stopped the execution of the current task.")
        }
        command.sendMessage(message)
    }

    @CommandMethod("reload-settings")
    @CommandDescription("Reloads the bot settings from disk")
    fun reloadSettingsCommand(
        command: ClientCommand,
    ) {
        ConfigHelper.reloadAllConfigs()

        val message = text {
            content("Successfully reloaded the settings.")
        }
        command.sendMessage(message)
    }
}
