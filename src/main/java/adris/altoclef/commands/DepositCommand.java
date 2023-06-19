package adris.altoclef.commands;

import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.commandsystem.ItemList;
import adris.altoclef.tasks.container.StoreInAnyContainerTask;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import org.apache.commons.lang3.ArrayUtils;

public class DepositCommand extends Command {
    public DepositCommand() throws CommandException {
        super("deposit", "Deposit ALL of our items", new Arg(ItemList.class, "items (empty for ALL non gear items)", null, 0, false));
    }

    public static ItemTarget[] getAllNonEquippedOrToolItemsAsTarget() {
        return StorageHelper.getAllInventoryItemsAsTargets(slot -> {
            // Ignore armor
            if (ArrayUtils.contains(PlayerSlot.ARMOR_SLOTS, slot))
                return false;
            ItemStack stack = StorageHelper.getItemStackInSlot(slot);
            // Ignore tools
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                return !(item instanceof ToolItem);
            }
            return false;
        });
    }

    @Override
    protected void call(ArgParser parser) throws CommandException {
        ItemList itemList = parser.get(ItemList.class);
        ItemTarget[] items;
        if (itemList == null) {
            items = getAllNonEquippedOrToolItemsAsTarget();
        } else {
            items = itemList.items;
        }

        AltoClef.INSTANCE.runUserTask(new StoreInAnyContainerTask(false, items), this::finish);
    }
}
