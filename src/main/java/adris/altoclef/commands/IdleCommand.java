package adris.altoclef.commands;

import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.movement.IdleTask;
import gay.solonovamax.altoclef.AltoClef;

public class IdleCommand extends Command {
    public IdleCommand() {
        super("idle", "Stand still");
    }

    @Override
    protected void call(ArgParser parser) {
        AltoClef.INSTANCE.runUserTask(new IdleTask(), this::finish);
    }
}
