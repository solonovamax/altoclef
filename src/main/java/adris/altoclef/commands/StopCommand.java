package adris.altoclef.commands;

import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import gay.solonovamax.altoclef.AltoClef;

public class StopCommand extends Command {

    public StopCommand() {
        super("stop", "Stop task runner (stops all automation)");
    }

    @Override
    protected void call(ArgParser parser) {
        AltoClef.INSTANCE.getUserTaskChain().cancel();
        finish();
    }
}
