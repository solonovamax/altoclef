package adris.altoclef.tasks.examples;

import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class ExampleTask2 extends Task {

    private BlockPos _target = null;

    @Override
    protected void onStart() {
        AltoClef.INSTANCE.getBlockTracker().trackBlock(Blocks.OAK_LOG);

        // Extra credit: Bot will NOT damage trees.
        AltoClef.INSTANCE.getBehaviour().push();
        AltoClef.INSTANCE.getBehaviour().avoidBlockBreaking(blockPos -> {
            BlockState s = AltoClef.INSTANCE.getWorld().getBlockState(blockPos);
            return s.getBlock() == Blocks.OAK_LEAVES || s.getBlock() == Blocks.OAK_LOG;
        });
    }

    @Override
    protected Task onTick() {
        /*
         * Find a tree
         * Go to its top (above the last leaf block)
         *
         * Locate the nearest log
         * Stand on top of its last leaf
         */

        if (_target != null) {
            return new GetToBlockTask(_target);
        }

        if (AltoClef.INSTANCE.getBlockTracker().anyFound(Blocks.OAK_LOG)) {
            Optional<BlockPos> nearest = AltoClef.INSTANCE.getBlockTracker().getNearestTracking(AltoClef.INSTANCE.getPlayer().getPos(), Blocks.OAK_LOG);
            if (nearest.isPresent()) {
                // Figure out leaves
                BlockPos check = new BlockPos(nearest.get());
                while (AltoClef.INSTANCE.getWorld().getBlockState(check).getBlock() == Blocks.OAK_LOG ||
                        AltoClef.INSTANCE.getWorld().getBlockState(check).getBlock() == Blocks.OAK_LEAVES) {
                    check = check.up();
                }
                _target = check;
            }
            return null;
        }

        return new TimeoutWanderTask();
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getBlockTracker().stopTracking(Blocks.OAK_LOG);
        AltoClef.INSTANCE.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof ExampleTask2;
    }

    @Override
    public boolean isFinished() {
        if (_target != null) {
            return AltoClef.INSTANCE.getPlayer().getBlockPos().equals(_target);
        }
        return super.isFinished();
    }

    @Override
    protected String toDebugString() {
        return "Standing on a tree";
    }
}
