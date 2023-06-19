package adris.altoclef.commands;

import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.util.helpers.WorldHelper;
import gay.solonovamax.altoclef.AltoClef;

public class CoordsCommand extends Command {
    public CoordsCommand() {
        super("coords", "Get bot's current coordinates");
    }

    @Override
    protected void call(ArgParser parser) {
        AltoClef.INSTANCE.log("CURRENT COORDINATES: " + AltoClef.INSTANCE.getPlayer().getBlockPos().toShortString() + " (Current dimension: " + WorldHelper.getCurrentDimension() + ")");
        finish();
    }
}
