package adris.altoclef.tasks.construction;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Predicate;

public class CoverWithSandTask extends Task {
    private static final TimerGame timer = new TimerGame(30);
    private static final Task getSand = TaskCatalogue.getItemTask(Items.SAND, 128);
    private static final Task goToNether = new DefaultGoToDimensionTask(Dimension.NETHER);
    private static final Task goToOverworld = new DefaultGoToDimensionTask(Dimension.OVERWORLD);
    private BlockPos lavaPos;

    @Override
    protected void onStart() {
        timer.reset();
        AltoClef.INSTANCE.getBlockTracker().trackBlock(Blocks.LAVA);
    }

    @Override
    protected Task onTick() {
        if (getSand != null && getSand.isActive() && !getSand.isFinished()) {
            setDebugState("Getting sands to cover nether lava.");
            timer.reset();
            return getSand;
        }
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD &&
                AltoClef.INSTANCE.getItemStorage().getItemCount(Items.SAND) < 64) {
            timer.reset();
            return getSand;
        }
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD &&
                AltoClef.INSTANCE.getItemStorage().getItemCount(Items.SAND) > 64) {
            setDebugState("Going to nether.");
            timer.reset();
            return goToNether;
        }
        if (WorldHelper.getCurrentDimension() == Dimension.NETHER &&
                !AltoClef.INSTANCE.getItemStorage().hasItem(Items.SAND)) {
            setDebugState("Going to overworld to get sand.");
            timer.reset();
            return goToOverworld;
        }
        if (coverLavaWithSand(AltoClef.INSTANCE) == null) {
            setDebugState("Searching valid lava.");
            timer.reset();
            return new TimeoutWanderTask();
        }
        setDebugState("Covering lava with sand");
        return coverLavaWithSand(AltoClef.INSTANCE);
    }

    private Task coverLavaWithSand(AltoClef mod) {
        Predicate<BlockPos> validLava = blockPos ->
                mod.getWorld().getBlockState(blockPos).getFluidState().isStill() &&
                        WorldHelper.isAir(mod, blockPos.up()) &&
                        (!WorldHelper.isBlock(blockPos.north(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.south(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.east(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.west(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.north().up(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.south().up(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.east().up(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.west().up(), Blocks.LAVA));
        Optional<BlockPos> lava = mod.getBlockTracker().getNearestTracking(validLava, Blocks.LAVA);
        if (lava.isPresent()) {
            if (lavaPos == null) {
                lavaPos = lava.get();
                timer.reset();
            }
            if (timer.elapsed()) {
                lavaPos = lava.get();
                timer.reset();
            }
            if (!WorldHelper.isBlock(lavaPos, Blocks.LAVA) || (!WorldHelper.isAir(mod, lavaPos.up()) &&
                    !WorldHelper.isFallingBlock(lavaPos.up())) ||
                    !mod.getWorld().getBlockState(lavaPos).getFluidState().isStill()) {
                lavaPos = lava.get();
                timer.reset();
            }
            return new PlaceBlockTask(lavaPos.up(), Blocks.SAND);
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getBlockTracker().stopTracking(Blocks.LAVA);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof CoverWithSandTask;
    }

    @Override
    protected String toDebugString() {
        return "Covering nether lava with sand";
    }
}
