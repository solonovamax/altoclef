package adris.altoclef.commands;

import adris.altoclef.butler.Butler;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.movement.FollowPlayerTask;
import gay.solonovamax.altoclef.AltoClef;

public class FollowCommand extends Command {
    public FollowCommand() throws CommandException {
        super("follow", "Follows you or someone else", new Arg(String.class, "username", null, 0));
    }

    @Override
    protected void call(ArgParser parser) throws CommandException {
        String username = parser.get(String.class);
        if (username == null) {
            Butler butler = AltoClef.INSTANCE.getButler();
            if (butler.hasCurrentUser()) {
                username = butler.getCurrentUser();
            } else {
                AltoClef.INSTANCE.logWarning("No butler user currently present. Running this command with no user argument can ONLY be done via butler.");
                finish();
                return;
            }
        }
        AltoClef.INSTANCE.runUserTask(new FollowPlayerTask(username), this::finish);
    }
}
