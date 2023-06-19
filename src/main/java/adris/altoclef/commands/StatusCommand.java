package adris.altoclef.commands;

import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasksystem.Task;
import gay.solonovamax.altoclef.AltoClef;

import java.util.List;

public class StatusCommand extends Command {
    public StatusCommand() {
        super("status", "Get status of currently executing command");
    }

    @Override
    protected void call(ArgParser parser) {
        List<Task> tasks = AltoClef.INSTANCE.getUserTaskChain().getTasks();
        if (tasks.size() == 0) {
            AltoClef.INSTANCE.log("No tasks currently running.");
        } else {
            AltoClef.INSTANCE.log("CURRENT TASK: " + tasks.get(0).toString());
        }
        finish();
    }
}
