package adris.altoclef.commands;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.ui.MessagePriority;
import adris.altoclef.util.helpers.ItemHelper;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashMap;

public class InventoryCommand extends Command {
    public InventoryCommand() throws CommandException {
        super("inventory", "Prints the bot's inventory OR returns how many of an item the bot has", new Arg(String.class, "item", null, 1));
    }

    @Override
    protected void call(ArgParser parser) throws CommandException {
        String item = parser.get(String.class);
        if (item == null) {
            // Print inventory
            // Get item counts
            HashMap<String, Integer> counts = new HashMap<>();
            PlayerInventory inventory = AltoClef.INSTANCE.getPlayer().getInventory();
            for (int i = 0; i < inventory.size(); ++i) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty()) {
                    String name = ItemHelper.stripItemName(stack.getItem());
                    if (!counts.containsKey(name)) counts.put(name, 0);
                    counts.put(name, counts.get(name) + stack.getCount());
                }
            }
            // Print
            AltoClef.INSTANCE.log("INVENTORY: ", MessagePriority.OPTIONAL);
            for (String name : counts.keySet()) {
                AltoClef.INSTANCE.log(name + " : " + counts.get(name), MessagePriority.OPTIONAL);
            }
            AltoClef.INSTANCE.log("(inventory list sent) ", MessagePriority.OPTIONAL);
        } else {
            // Print item quantity
            Item[] matches = TaskCatalogue.getItemMatches(item);
            if (matches == null || matches.length == 0) {
                AltoClef.INSTANCE.logWarning("Item \"" + item + "\" is not catalogued/recognized.");
                finish();
                return;
            }
            int count = AltoClef.INSTANCE.getItemStorage().getItemCount(matches);
            if (count == 0) {
                AltoClef.INSTANCE.log(item + " COUNT: (none)");
            } else {
                AltoClef.INSTANCE.log(item + " COUNT: " + count);
            }
        }
        finish();
    }
}
