package adris.altoclef.tasks.construction;

import adris.altoclef.Debug;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.BlockPlaceEvent;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Place a type of block nearby, anywhere.
 * <p>
 * Also known as the "bear strats" task.
 */
public class PlaceBlockNearbyTask extends Task {

    private final Block[] _toPlace;

    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private final TimeoutWanderTask _wander = new TimeoutWanderTask(5);

    private final TimerGame _randomlookTimer = new TimerGame(0.25);
    private final Predicate<BlockPos> _canPlaceHere;
    private BlockPos _justPlaced; // Where we JUST placed a block.
    private BlockPos _tryPlace;   // Where we should TRY placing a block.
    // Oof, necesarry for the onBlockPlaced action.
    private AltoClef _mod;
    private Subscription<BlockPlaceEvent> _onBlockPlaced;

    public PlaceBlockNearbyTask(Predicate<BlockPos> canPlaceHere, Block... toPlace) {
        _toPlace = toPlace;
        _canPlaceHere = canPlaceHere;
    }

    public PlaceBlockNearbyTask(Block... toPlace) {
        this(blockPos -> true, toPlace);
    }

    @Override
    protected void onStart() {
        _progressChecker.reset();
        _mod = AltoClef.INSTANCE;
        AltoClef.INSTANCE.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);

        // Check for blocks being placed
        _onBlockPlaced = EventBus.subscribe(BlockPlaceEvent.class, evt -> {
            if (ArrayUtils.contains(_toPlace, evt.blockState.getBlock())) {
                stopPlacing();
            }
        });
    }

    @Override
    protected Task onTick() {
        if (AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
            _progressChecker.reset();
        }
        // Method:
        // - If looking at placable block
        //      Place immediately
        // Find a spot to place
        // - Prefer flat areas (open space, block below) closest to player
        // -

        // Close screen first
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            if (moveTo.isPresent()) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            if (ItemHelper.canThrowAwayStack(AltoClef.INSTANCE, cursorStack)) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return null;
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(AltoClef.INSTANCE);
            // Try throwing away cursor slot if it's garbage
            if (garbage.isPresent()) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        }

        // Try placing where we're looking right now.
        BlockPos current = getCurrentlyLookingBlockPlace();
        if (current != null && _canPlaceHere.test(current)) {
            setDebugState("Placing since we can...");
            if (AltoClef.INSTANCE.getSlotHandler().forceEquipItem(ItemHelper.blocksToItems(_toPlace))) {
                if (place(current)) {
                    return null;
                }
            }
        }

        // Wander while we can.
        if (_wander.isActive() && !_wander.isFinished()) {
            setDebugState("Wandering, will try to place again later.");
            _progressChecker.reset();
            return _wander;
        }
        // Fail check
        if (!_progressChecker.check(AltoClef.INSTANCE)) {
            Debug.logMessage("Failed placing, wandering and trying again.");
            LookHelper.randomOrientation(AltoClef.INSTANCE);
            if (_tryPlace != null) {
                AltoClef.INSTANCE.getBlockTracker().requestBlockUnreachable(_tryPlace);
                _tryPlace = null;
            }
            return _wander;
        }

        // Try to place at a particular spot.
        if (_tryPlace == null || !WorldHelper.canReach(AltoClef.INSTANCE, _tryPlace)) {
            _tryPlace = locateClosePlacePos();
        }
        if (_tryPlace != null) {
            setDebugState("Trying to place at " + _tryPlace);
            _justPlaced = _tryPlace;
            return new PlaceBlockTask(_tryPlace, _toPlace);
        }

        // Look in random places to maybe get a random hit
        if (_randomlookTimer.elapsed()) {
            _randomlookTimer.reset();
            LookHelper.randomOrientation(AltoClef.INSTANCE);
        }

        setDebugState("Wandering until we randomly place or find a good place spot.");
        return new TimeoutWanderTask();
    }

    @Override
    protected void onStop(Task interruptTask) {
        stopPlacing();
        EventBus.unsubscribe(_onBlockPlaced);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof PlaceBlockNearbyTask task) {
            return Arrays.equals(task._toPlace, _toPlace);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Place " + Arrays.toString(_toPlace) + " nearby";
    }

    @Override
    public boolean isFinished() {
        return _justPlaced != null && ArrayUtils.contains(_toPlace, AltoClef.INSTANCE.getWorld().getBlockState(_justPlaced).getBlock());
    }

    public BlockPos getPlaced() {
        return _justPlaced;
    }

    private BlockPos getCurrentlyLookingBlockPlace() {
        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        if (hit instanceof BlockHitResult bhit) {
            BlockPos bpos = bhit.getBlockPos();//.subtract(bhit.getSide().getVector());
            // Debug.logMessage("TEMP: A: " + bpos);
            IPlayerContext ctx = AltoClef.INSTANCE.getClientBaritone().getPlayerContext();
            if (MovementHelper.canPlaceAgainst(ctx, bpos)) {
                BlockPos placePos = bhit.getBlockPos().add(bhit.getSide().getVector());
                // Don't place inside the player.
                if (WorldHelper.isInsidePlayer(AltoClef.INSTANCE, placePos)) {
                    return null;
                }
                // Debug.logMessage("TEMP: B (actual): " + placePos);
                if (WorldHelper.canPlace(AltoClef.INSTANCE, placePos)) {
                    return placePos;
                }
            }
        }
        return null;
    }

    private boolean blockEquipped() {
        return StorageHelper.isEquipped(AltoClef.INSTANCE, ItemHelper.blocksToItems(_toPlace));
    }

    private boolean place(BlockPos targetPlace) {
        if (!AltoClef.INSTANCE.getExtraBaritoneSettings().isInteractionPaused() && blockEquipped()) {
            // Shift click just for 100% container security.
            AltoClef.INSTANCE.getInputControls().hold(Input.SNEAK);

            // mod.getInputControls().tryPress(Input.CLICK_RIGHT);
            // This appears to work on servers...
            // TODO: Helper lol
            HitResult mouseOver = MinecraftClient.getInstance().crosshairTarget;
            if (mouseOver == null || mouseOver.getType() != HitResult.Type.BLOCK) {
                return false;
            }
            Hand hand = Hand.MAIN_HAND;
            assert MinecraftClient.getInstance().interactionManager != null;
            if (MinecraftClient.getInstance().interactionManager.interactBlock(AltoClef.INSTANCE.getPlayer(), hand, (BlockHitResult) mouseOver) == ActionResult.SUCCESS &&
                    AltoClef.INSTANCE.getPlayer().isSneaking()) {
                AltoClef.INSTANCE.getPlayer().swingHand(hand);
                _justPlaced = targetPlace;
                Debug.logMessage("PRESSED");
                return true;
            }

            // mod.getControllerExtras().mouseClickOverride(1, true);
            // mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
            return true;
        }
        return false;
    }

    private void stopPlacing() {
        AltoClef.INSTANCE.getInputControls().release(Input.SNEAK);
        // mod.getControllerExtras().mouseClickOverride(1, false);
        // Oof, these sometimes cause issues so this is a bit of a duct tape fix.
        AltoClef.INSTANCE.getClientBaritone().getBuilderProcess().onLostControl();
    }

    private BlockPos locateClosePlacePos() {
        int range = 7;
        BlockPos best = null;
        double smallestScore = Double.POSITIVE_INFINITY;
        BlockPos start = AltoClef.INSTANCE.getPlayer().getBlockPos().add(-range, -range, -range);
        BlockPos end = AltoClef.INSTANCE.getPlayer().getBlockPos().add(range, range, range);
        for (BlockPos blockPos : WorldHelper.scanRegion(start, end)) {
            boolean solid = WorldHelper.isSolid(AltoClef.INSTANCE, blockPos);
            boolean inside = WorldHelper.isInsidePlayer(AltoClef.INSTANCE, blockPos);
            // We can't break this block.
            if (solid && !WorldHelper.canBreak(AltoClef.INSTANCE, blockPos)) {
                continue;
            }
            // We can't place here as defined by user.
            if (!_canPlaceHere.test(blockPos)) {
                continue;
            }
            // We can't place here.
            if (!WorldHelper.canReach(AltoClef.INSTANCE, blockPos) || !WorldHelper.canPlace(AltoClef.INSTANCE, blockPos)) {
                continue;
            }
            boolean hasBelow = WorldHelper.isSolid(AltoClef.INSTANCE, blockPos.down());
            double distSq = blockPos.getSquaredDistance(AltoClef.INSTANCE.getPlayer().getPos());

            double score = distSq + (solid ? 4 : 0) + (hasBelow ? 0 : 10) + (inside ? 3 : 0);

            if (score < smallestScore) {
                best = blockPos;
                smallestScore = score;
            }
        }

        return best;
    }
}
