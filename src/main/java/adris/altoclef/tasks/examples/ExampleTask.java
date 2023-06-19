package adris.altoclef.tasks.examples;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class ExampleTask extends Task {

    private final int _numberOfStonePickaxesToGrab;
    private final BlockPos _whereToPlaceCobblestone;

    public ExampleTask(int numberOfStonePickaxesToGrab, BlockPos whereToPlaceCobblestone) {
        _numberOfStonePickaxesToGrab = numberOfStonePickaxesToGrab;
        _whereToPlaceCobblestone = whereToPlaceCobblestone;
    }

    @Override
    protected void onStart() {
        AltoClef.INSTANCE.getBehaviour().push();
        AltoClef.INSTANCE.getBehaviour().addProtectedItems(Items.COBBLESTONE);
    }

    @Override
    protected Task onTick() {
        /*
         * Grab X stone pickaxes
         * Make sure we have a block
         * Then, place the block.
         */

        if (AltoClef.INSTANCE.getItemStorage().getItemCount(Items.STONE_PICKAXE) < _numberOfStonePickaxesToGrab) {
            return TaskCatalogue.getItemTask(Items.STONE_PICKAXE, _numberOfStonePickaxesToGrab);
        }

        if (!AltoClef.INSTANCE.getItemStorage().hasItem(Items.COBBLESTONE)) {
            return TaskCatalogue.getItemTask(Items.COBBLESTONE, 1);
        }

        if (AltoClef.INSTANCE.getChunkTracker().isChunkLoaded(_whereToPlaceCobblestone)) {
            if (AltoClef.INSTANCE.getWorld().getBlockState(_whereToPlaceCobblestone).getBlock() != Blocks.COBBLESTONE) {
                return new PlaceBlockTask(_whereToPlaceCobblestone, Blocks.COBBLESTONE); ///new PlaceStructureBlockTask(_whereToPlaceCobblestone);
            }
            return null;
        } else {
            return new GetToBlockTask(_whereToPlaceCobblestone);
        }
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getBehaviour().pop();
    }

    @Override
    public boolean isFinished() {
        return AltoClef.INSTANCE.getItemStorage().getItemCount(Items.STONE_PICKAXE) >= _numberOfStonePickaxesToGrab &&
                AltoClef.INSTANCE.getWorld().getBlockState(_whereToPlaceCobblestone).getBlock() == Blocks.COBBLESTONE;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ExampleTask) {
            ExampleTask task = (ExampleTask) other;
            return task._numberOfStonePickaxesToGrab == _numberOfStonePickaxesToGrab
                    && task._whereToPlaceCobblestone.equals(_whereToPlaceCobblestone);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Boofin";
    }
}
