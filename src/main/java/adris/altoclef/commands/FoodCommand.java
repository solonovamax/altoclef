package adris.altoclef.commands;

import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.resources.CollectFoodTask;
import gay.solonovamax.altoclef.AltoClef;

public class FoodCommand extends Command {
    public FoodCommand() throws CommandException {
        super("food", "Collects a certain amount of food", new Arg<>(Integer.class, "count"));
    }

    @Override
    protected void call(ArgParser parser) throws CommandException {
        AltoClef.INSTANCE.runUserTask(new CollectFoodTask(parser.get(Integer.class)), this::finish);
    }
}
