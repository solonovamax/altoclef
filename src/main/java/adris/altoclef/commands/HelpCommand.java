package adris.altoclef.commands;

import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.ui.MessagePriority;
import gay.solonovamax.altoclef.AltoClef;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Lists all commands");
    }

    @Override
    protected void call(ArgParser parser) {
        AltoClef.INSTANCE.log("########## HELP: ##########", MessagePriority.OPTIONAL);
        int padSize = 10;
        for (Command c : AltoClef.INSTANCE.getCommandExecutor().allCommands()) {
            StringBuilder line = new StringBuilder();
            // line.append("");
            line.append(c.getName()).append(": ");
            int toAdd = padSize - c.getName().length();
            for (int i = 0; i < toAdd; ++i) {
                line.append(" ");
            }
            line.append(c.getDescription());
            AltoClef.INSTANCE.log(line.toString(), MessagePriority.OPTIONAL);
        }
        AltoClef.INSTANCE.log("###########################", MessagePriority.OPTIONAL);
        finish();
    }
}
