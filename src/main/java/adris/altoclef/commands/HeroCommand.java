package adris.altoclef.commands;

import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.entity.HeroTask;
import gay.solonovamax.altoclef.AltoClef;

public class HeroCommand extends Command {
    public HeroCommand() {
        super("hero", "Kill all hostile mobs.");
    }

    @Override
    protected void call(ArgParser parser) throws CommandException {
        AltoClef.INSTANCE.runUserTask(new HeroTask(), this::finish);
    }
}
