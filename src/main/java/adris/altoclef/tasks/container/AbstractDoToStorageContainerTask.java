package adris.altoclef.tasks.container;

import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.util.helpers.WorldHelper;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Opens a STORAGE container and does whatever you want inside of it
 */
public abstract class AbstractDoToStorageContainerTask extends Task {

    private ContainerType _currentContainerType = null;

    @Override
    protected void onStart() {

    }

    @Override
    protected Task onTick() {
        Optional<BlockPos> containerTarget = getContainerTarget();

        // No container found
        if (containerTarget.isEmpty()) {
            setDebugState("Wandering");
            _currentContainerType = null;
            return onSearchWander(AltoClef.INSTANCE);
        }

        BlockPos targetPos = containerTarget.get();

        // We're open
        if (_currentContainerType != null && ContainerType.screenHandlerMatches(_currentContainerType)) {

            // Optional<BlockPos> lastInteracted = mod.getItemStorage().getLastBlockPosInteraction();
            // if (lastInteracted.isPresent() && lastInteracted.get().equals(targetPos)) {
            Optional<ContainerCache> cache = AltoClef.INSTANCE.getItemStorage().getContainerAtPosition(targetPos);
            if (cache.isPresent()) {
                return onContainerOpenSubtask(AltoClef.INSTANCE, cache.get());
            }
            //}
        }

        // Get to the container
        if (AltoClef.INSTANCE.getChunkTracker().isChunkLoaded(targetPos)) {
            Block type = AltoClef.INSTANCE.getWorld().getBlockState(targetPos).getBlock();
            _currentContainerType = ContainerType.getFromBlock(type);
        }
        if (WorldHelper.isChest(AltoClef.INSTANCE, targetPos) && WorldHelper.isSolid(AltoClef.INSTANCE, targetPos.up()) && WorldHelper.canBreak(AltoClef.INSTANCE, targetPos.up())) {
            setDebugState("Clearing block above chest");
            return new DestroyBlockTask(targetPos.up());
        }
        setDebugState("Opening container: " + targetPos.toShortString());
        return new InteractWithBlockTask(targetPos);
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    protected abstract Optional<BlockPos> getContainerTarget();

    protected abstract Task onContainerOpenSubtask(AltoClef mod, ContainerCache containerCache);

    // Virtual
    // TODO: Interface this
    protected Task onSearchWander(AltoClef mod) {
        return new TimeoutWanderTask();
    }
}
