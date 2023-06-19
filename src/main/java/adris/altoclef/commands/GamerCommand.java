package adris.altoclef.commands;

import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.speedrun.BeatMinecraft2Task;
import gay.solonovamax.altoclef.AltoClef;

public class GamerCommand extends Command {
    public GamerCommand() {
        super("gamer", "Beats the game");
    }

    @Override
    protected void call(ArgParser parser) {
        AltoClef.INSTANCE.runUserTask(new BeatMinecraft2Task(), this::finish);
    }
}
