package adris.altoclef.commands;

import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.movement.GoToStrongholdPortalTask;
import adris.altoclef.tasks.movement.LocateDesertTempleTask;
import gay.solonovamax.altoclef.AltoClef;

public class LocateStructureCommand extends Command {

    public LocateStructureCommand() throws CommandException {
        super("locate_structure", "Locate a world generated structure.", new Arg(Structure.class, "structure"));
    }

    @Override
    protected void call(ArgParser parser) throws CommandException {
        Structure structure = parser.get(Structure.class);
        switch (structure) {
            case STRONGHOLD:
                AltoClef.INSTANCE.runUserTask(new GoToStrongholdPortalTask(1), this::finish);
                break;
            case DESERT_TEMPLE:
                AltoClef.INSTANCE.runUserTask(new LocateDesertTempleTask(), this::finish);
                break;
        }
    }

    public enum Structure {
        DESERT_TEMPLE,
        STRONGHOLD
    }
}
