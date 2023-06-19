package adris.altoclef.commands;

import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.entity.KillPlayerTask;
import gay.solonovamax.altoclef.AltoClef;

public class PunkCommand extends Command {
    public PunkCommand() throws CommandException {
        super("punk", "Punk 'em", new Arg(String.class, "playerName"));
    }

    @Override
    protected void call(ArgParser parser) throws CommandException {
        String playerName = parser.get(String.class);
        AltoClef.INSTANCE.runUserTask(new KillPlayerTask(playerName), this::finish);
    }
}
