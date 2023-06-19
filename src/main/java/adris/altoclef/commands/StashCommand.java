package adris.altoclef.commands;

import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.commandsystem.ItemList;
import adris.altoclef.tasks.container.StoreInStashTask;
import adris.altoclef.util.BlockRange;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;

public class StashCommand extends Command {
    public StashCommand() throws CommandException {
        // stash <stash_x> <stash_y> <stash_z> <stash_radius> [item list]
        super("stash", "Store an item in a chest/container stash. Will deposit ALL non-equipped items if item list is empty.",
                new Arg(Integer.class, "x_start"),
                new Arg(Integer.class, "y_start"),
                new Arg(Integer.class, "z_start"),
                new Arg(Integer.class, "x_end"),
                new Arg(Integer.class, "y_end"),
                new Arg(Integer.class, "z_end"),
                new Arg(ItemList.class, "items (empty for ALL)", null, 6, false));
    }

    @Override
    protected void call(ArgParser parser) throws CommandException {
        BlockPos start = new BlockPos(
                parser.get(Integer.class),
                parser.get(Integer.class),
                parser.get(Integer.class)
        );
        BlockPos end = new BlockPos(
                parser.get(Integer.class),
                parser.get(Integer.class),
                parser.get(Integer.class)
        );

        ItemList itemList = parser.get(ItemList.class);
        ItemTarget[] items;
        if (itemList == null) {
            items = DepositCommand.getAllNonEquippedOrToolItemsAsTarget();
        } else {
            items = itemList.items;
        }


        AltoClef.INSTANCE.runUserTask(new StoreInStashTask(true, new BlockRange(start, end, WorldHelper.getCurrentDimension()), items), this::finish);
    }
}
