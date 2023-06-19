package adris.altoclef.commands;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.ui.MessagePriority;
import gay.solonovamax.altoclef.AltoClef;

import java.util.Arrays;

public class ListCommand extends Command {
    public ListCommand() {
        super("list", "List all obtainable items");
    }

    @Override
    protected void call(ArgParser parser) throws CommandException {
        AltoClef.INSTANCE.log("#### LIST OF ALL OBTAINABLE ITEMS ####", MessagePriority.OPTIONAL);
        AltoClef.INSTANCE.log(Arrays.toString(TaskCatalogue.resourceNames().toArray()), MessagePriority.OPTIONAL);
        AltoClef.INSTANCE.log("############# END LIST ###############", MessagePriority.OPTIONAL);
    }
}
