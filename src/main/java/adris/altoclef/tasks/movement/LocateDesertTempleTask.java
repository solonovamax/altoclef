package adris.altoclef.tasks.movement;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;

public class LocateDesertTempleTask extends Task {

    private BlockPos _finalPos;

    @Override
    protected void onStart() {
        // Track desert pyramid blocks
        AltoClef.INSTANCE.getBlockTracker().trackBlock(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected Task onTick() {
        BlockPos desertTemplePos = WorldHelper.getADesertTemple(AltoClef.INSTANCE);
        if (desertTemplePos != null) {
            _finalPos = desertTemplePos.up(14);
        }
        if (_finalPos != null) {
            setDebugState("Going to found desert temple");
            return new GetToBlockTask(_finalPos, false);
        }
        return new SearchWithinBiomeTask(BiomeKeys.DESERT);
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getBlockTracker().stopTracking(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof LocateDesertTempleTask;
    }

    @Override
    protected String toDebugString() {
        return "Searchin' for temples";
    }

    @Override
    public boolean isFinished() {
        return AltoClef.INSTANCE.getPlayer().getBlockPos().equals(_finalPos);
    }
}
