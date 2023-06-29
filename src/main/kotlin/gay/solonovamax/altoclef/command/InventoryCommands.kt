package gay.solonovamax.altoclef.command

import adris.altoclef.tasks.container.StoreInAnyContainerTask
import adris.altoclef.tasks.container.StoreInStashTask
import adris.altoclef.util.BlockRange
import adris.altoclef.util.ItemTarget
import adris.altoclef.util.helpers.WorldHelper
import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import gay.solonovamax.altoclef.AltoClef
import gay.solonovamax.altoclef.command.source.ClientCommand
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import net.minecraft.command.argument.ItemPredicateArgumentType.ItemStackPredicateArgument as ItemStackPredicate

@CommandMethod("altoclef")
class InventoryCommands {
    @CommandMethod("deposit [item]")
    @CommandDescription("Deposits items in a container.")
    fun depositCommand(
        command: ClientCommand,
        @Argument("item", description = "Item type to deposit")
        itemPredicate: ItemStackPredicate?, // TODO Make this a Set<ItemStackPredicateArgument>
    ) {
        // TODO only loop through items in inventory
        val validItems = Registries.ITEM.filter { item ->
            itemPredicate?.test(item.defaultStack) ?: true
        }
        val itemTarget = ItemTarget(validItems.toTypedArray())
        itemTarget.infinite()

        AltoClef.runUserTask(StoreInAnyContainerTask(false, itemTarget))
    }

    @CommandMethod("stash <startX> <startY> <startZ> <endX> <endY> <endZ> [item]")
    fun stashCommand(
        command: ClientCommand,
        @Argument("startX", description = "X coordinate of the start position to stash")
        startX: Int,
        @Argument("startY", description = "Y coordinate of the start position to stash")
        startY: Int,
        @Argument("startZ", description = "Z coordinate of the start position to stash")
        startZ: Int,
        @Argument("endX", description = "X coordinate of the end position to stash")
        endX: Int,
        @Argument("endY", description = "Y coordinate of the end position to stash")
        endY: Int,
        @Argument("endZ", description = "Z coordinate of the end position to stash")
        endZ: Int,
        @Argument("item", description = "Item type to stash")
        itemPredicate: ItemStackPredicate?,
    ) {
        val validItems = Registries.ITEM.filter { item ->
            itemPredicate?.test(item.defaultStack) ?: true
        }
        val itemTarget = ItemTarget(validItems.toTypedArray())
        itemTarget.infinite()

        val start = BlockPos(startX, startY, startZ)
        val end = BlockPos(endX, endY, endZ)

        AltoClef.runUserTask(
            StoreInStashTask(
                true,
                BlockRange(start, end, WorldHelper.getCurrentDimension()),
                itemTarget,
            )
        )
    }
}
