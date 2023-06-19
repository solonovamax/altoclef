package adris.altoclef.tasks.container;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Block;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Optional;


/**
 * Interacts with a container, obtaining and placing one if none were found nearby.
 */
public abstract class DoStuffInContainerTask extends Task {

    private final ItemTarget _containerTarget;
    private final Block[] _containerBlocks;

    private final PlaceBlockNearbyTask _placeTask;
    // If we decided on placing, force place for at least 10 seconds
    private final TimerGame _placeForceTimer = new TimerGame(10);
    // If we just placed something, stop placing and try going to the nearest container.
    private final TimerGame _justPlacedTimer = new TimerGame(3);
    private BlockPos _cachedContainerPosition = null;
    private Task _openTableTask;

    public DoStuffInContainerTask(Block[] containerBlocks, ItemTarget containerTarget) {
        _containerBlocks = containerBlocks;
        _containerTarget = containerTarget;

        _placeTask = new PlaceBlockNearbyTask(_containerBlocks);
    }

    public DoStuffInContainerTask(Block containerBlock, ItemTarget containerTarget) {
        this(new Block[]{containerBlock}, containerTarget);
    }

    @Override
    protected void onStart() {
        AltoClef.INSTANCE.getBehaviour().push();
        if (_openTableTask == null) {
            _openTableTask = new DoToClosestBlockTask(InteractWithBlockTask::new, _containerBlocks);
        }

        AltoClef.INSTANCE.getBlockTracker().trackBlock(_containerBlocks);

        // Protect container since we might place it.
        AltoClef.INSTANCE.getBehaviour().addProtectedItems(ItemHelper.blocksToItems(_containerBlocks));
    }

    @Override
    protected Task onTick() {
        // If we're placing, keep on placing.
        if (AltoClef.INSTANCE.getItemStorage().hasItem(ItemHelper.blocksToItems(_containerBlocks)) && _placeTask.isActive() && !_placeTask.isFinished()) {
            setDebugState("Placing container");
            return _placeTask;
        }

        if (isContainerOpen(AltoClef.INSTANCE)) {
            return containerSubTask(AltoClef.INSTANCE);
        }

        // infinity if such a container does not exist.
        double costToWalk = Double.POSITIVE_INFINITY;

        Optional<BlockPos> nearest;

        Vec3d currentPos = AltoClef.INSTANCE.getPlayer().getPos();
        BlockPos override = overrideContainerPosition(AltoClef.INSTANCE);

        if (override != null && AltoClef.INSTANCE.getBlockTracker().blockIsValid(override, _containerBlocks)) {
            // We have an override so go there instead.
            nearest = Optional.of(override);
        } else {
            // Track nearest container
            nearest = AltoClef.INSTANCE.getBlockTracker().getNearestTracking(currentPos, blockPos -> WorldHelper.canReach(AltoClef.INSTANCE, blockPos), _containerBlocks);
        }
        if (nearest.isEmpty()) {
            // If all else fails, try using our placed task
            nearest = Optional.ofNullable(_placeTask.getPlaced());
            if (nearest.isPresent() && !AltoClef.INSTANCE.getBlockTracker().blockIsValid(nearest.get(), _containerBlocks)) {
                nearest = Optional.empty();
            }
        }
        if (nearest.isPresent()) {
            costToWalk = BaritoneHelper.calculateGenericHeuristic(currentPos, WorldHelper.toVec3d(nearest.get()));
        }

        // Make a new container if going to the container is a pretty bad cost.
        // Also keep on making the container if we're stuck in some
        if (costToWalk > getCostToMakeNew(AltoClef.INSTANCE)) {
            _placeForceTimer.reset();
        }
        if (nearest.isEmpty() || (!_placeForceTimer.elapsed() && _justPlacedTimer.elapsed())) {
            // It's cheaper to make a new one, or our only option.

            // We're no longer going to our previous container.
            _cachedContainerPosition = null;

            // Get if we don't have...
            if (!AltoClef.INSTANCE.getItemStorage().hasItem(_containerTarget)) {
                setDebugState("Getting container item");
                return TaskCatalogue.getItemTask(_containerTarget);
            }

            setDebugState("Placing container...");

            _justPlacedTimer.reset();
            // Now place!
            return _placeTask;
        }

        // This is insanely cursed.
        // TODO: Finish committing to optionals, this is ugly.
        _cachedContainerPosition = nearest.get();

        // Walk to it and open it

        // Wait for food
        if (AltoClef.INSTANCE.getFoodChain().needsToEat()) {
            setDebugState("Waiting for eating...");
            return null;
        }
        setDebugState("Walking to container... " + nearest.get().toShortString());

        if (!StorageHelper.getItemStackInCursorSlot().isEmpty()) {
            Optional<Slot> toMoveTo = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(StorageHelper.getItemStackInCursorSlot(), false);
            if (toMoveTo.isEmpty()) {
                return new EnsureFreeInventorySlotTask();
            }
            if (ItemHelper.canThrowAwayStack(AltoClef.INSTANCE, StorageHelper.getItemStackInCursorSlot())) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return null;
            }
            AltoClef.INSTANCE.getSlotHandler().clickSlot(toMoveTo.get(), 0, SlotActionType.PICKUP);
            return null;
        }
        return _openTableTask;
        //return new GetToBlockTask(nearest, true);
    }

    public ItemTarget getContainerTarget() {
        return _containerTarget;
    }

    // Virtual
    protected BlockPos overrideContainerPosition(AltoClef mod) {
        return null;
    }

    protected BlockPos getTargetContainerPosition() {
        return _cachedContainerPosition;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getBehaviour().pop();
        AltoClef.INSTANCE.getBlockTracker().stopTracking(_containerBlocks);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DoStuffInContainerTask task) {
            if (!Arrays.equals(task._containerBlocks, _containerBlocks)) return false;
            if (!task._containerTarget.equals(_containerTarget)) return false;
            return isSubTaskEqual(task);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Doing stuff in " + _containerTarget + " container";
    }

    protected abstract boolean isSubTaskEqual(DoStuffInContainerTask other);

    protected abstract boolean isContainerOpen(AltoClef mod);

    protected abstract Task containerSubTask(AltoClef mod);

    protected abstract double getCostToMakeNew(AltoClef mod);
}
