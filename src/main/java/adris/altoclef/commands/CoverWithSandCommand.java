package adris.altoclef.commands;

import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.construction.CoverWithSandTask;
import gay.solonovamax.altoclef.AltoClef;

public class CoverWithSandCommand extends Command {
    public CoverWithSandCommand() {
        super("coverwithsand", "Cover nether lava with sand.");
    }

    @Override
    protected void call(ArgParser parser) throws CommandException {
        AltoClef.INSTANCE.runUserTask(new CoverWithSandTask(), this::finish);
    }
}
