package adris.altoclef;

import adris.altoclef.commands.EquipCommand;
import adris.altoclef.commands.FollowCommand;
import adris.altoclef.commands.FoodCommand;
import adris.altoclef.commands.GetCommand;
import adris.altoclef.commands.GiveCommand;
import adris.altoclef.commands.GotoCommand;
import adris.altoclef.commands.InventoryCommand;
import adris.altoclef.commands.TestCommand;
import adris.altoclef.commandsystem.CommandException;
import gay.solonovamax.altoclef.AltoClef;

/**
 * Initializes altoclef's built in commands.
 */
public class AltoClefCommands {

    public AltoClefCommands() throws CommandException {
        // List commands here
        AltoClef.INSTANCE.getCommandExecutor().registerNewCommand(
                new GetCommand(),
                new FollowCommand(),
                new GiveCommand(),
                new EquipCommand(),
                new GotoCommand(),
                new InventoryCommand(),
                new TestCommand(),
                new FoodCommand()
                // new TestMoveInventoryCommand(),
                //    new TestSwapInventoryCommand()
        );
    }
}
