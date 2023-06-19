package adris.altoclef.commands;

import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.speedrun.MarvionBeatMinecraftTask;
import gay.solonovamax.altoclef.AltoClef;

public class MarvionCommand extends Command {
    public MarvionCommand() {
        super("marvion", "Beats the game (Marvion version).");
    }

    @Override
    protected void call(ArgParser parser) {
        AltoClef.INSTANCE.runUserTask(new MarvionBeatMinecraftTask(), this::finish);
    }
}
