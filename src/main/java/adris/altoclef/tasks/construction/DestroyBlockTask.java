package adris.altoclef.tasks.construction;

import adris.altoclef.Debug;
import adris.altoclef.tasks.movement.RunAwayFromPositionTask;
import adris.altoclef.tasks.movement.SafeRandomShimmyTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.Slot;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Destroy a block at a position.
 */
public class DestroyBlockTask extends Task implements ITaskRequiresGrounded {
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final MovementProgressChecker _moveChecker = new MovementProgressChecker();
    private final BlockPos _pos;
    Block[] annoyingBlocks = new Block[]{
            Blocks.VINE,
            Blocks.NETHER_SPROUTS,
            Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT,
            Blocks.TWISTING_VINES,
            Blocks.TWISTING_VINES_PLANT,
            Blocks.WEEPING_VINES_PLANT,
            Blocks.LADDER,
            Blocks.BIG_DRIPLEAF,
            Blocks.BIG_DRIPLEAF_STEM,
            Blocks.SMALL_DRIPLEAF,
            Blocks.TALL_GRASS,
            Blocks.GRASS,
            Blocks.SWEET_BERRY_BUSH
    };
    private Task _unstuckTask = null;
    private boolean isMining;

    public DestroyBlockTask(BlockPos pos) {
        _pos = pos;
    }

    private static BlockPos[] generateSides(BlockPos pos) {
        return new BlockPos[]{
                pos.add(1, 0, 0),
                pos.add(-1, 0, 0),
                pos.add(0, 0, 1),
                pos.add(0, 0, -1),
                pos.add(1, 0, -1),
                pos.add(1, 0, 1),
                pos.add(-1, 0, -1),
                pos.add(-1, 0, 1)
        };
    }

    private boolean isAnnoying(AltoClef mod, BlockPos pos) {
        for (Block AnnoyingBlocks : annoyingBlocks) {
            return mod.getWorld().getBlockState(pos).getBlock() == AnnoyingBlocks ||
                    mod.getWorld().getBlockState(pos).getBlock() instanceof DoorBlock ||
                    mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock ||
                    mod.getWorld().getBlockState(pos).getBlock() instanceof FenceGateBlock ||
                    mod.getWorld().getBlockState(pos).getBlock() instanceof FlowerBlock;
        }
        return false;
    }

    // This happens all the time in mineshafts and swamps/jungles
    private BlockPos stuckInBlock(AltoClef mod) {
        BlockPos p = mod.getPlayer().getBlockPos();
        if (isAnnoying(mod, p)) return p;
        if (isAnnoying(mod, p.up())) return p.up();
        BlockPos[] toCheck = generateSides(p);
        for (BlockPos check : toCheck) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }
        BlockPos[] toCheckHigh = generateSides(p.up());
        for (BlockPos check : toCheckHigh) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }
        return null;
    }

    private Task getFenceUnstuckTask() {
        return new SafeRandomShimmyTask();
    }

    @Override
    protected void onStart() {
        AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().forceCancel();
        _moveChecker.reset();
        stuckCheck.reset();
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> AltoClef.INSTANCE.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            if (ItemHelper.canThrowAwayStack(AltoClef.INSTANCE, cursorStack)) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(AltoClef.INSTANCE);
            // Try throwing away cursor slot if it's garbage
            garbage.ifPresent(slot -> AltoClef.INSTANCE.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        }
    }

    @Override
    protected Task onTick() {
        if (AltoClef.INSTANCE.getWorld().getBlockState(_pos).getBlock() == Blocks.WHITE_WOOL) {
            Iterable<Entity> entities = AltoClef.INSTANCE.getWorld().getEntities();
            for (Entity entity : entities) {
                if (entity instanceof PillagerEntity) {
                    if (_pos.isWithinDistance(entity.getPos(), 144)) {
                        Debug.logMessage("Blacklisting pillager wool.");
                        AltoClef.INSTANCE.getBlockTracker().requestBlockUnreachable(_pos, 0);
                    }
                }
            }
        }
        if (AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
            _moveChecker.reset();
        }
        if (WorldHelper.isInNetherPortal(AltoClef.INSTANCE)) {
            if (!AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                AltoClef.INSTANCE.getInputControls().hold(Input.SNEAK);
                AltoClef.INSTANCE.getInputControls().hold(Input.MOVE_FORWARD);
                return null;
            } else {
                AltoClef.INSTANCE.getInputControls().release(Input.SNEAK);
                AltoClef.INSTANCE.getInputControls().release(Input.MOVE_BACK);
                AltoClef.INSTANCE.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else {
            if (AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
                AltoClef.INSTANCE.getInputControls().release(Input.SNEAK);
                AltoClef.INSTANCE.getInputControls().release(Input.MOVE_BACK);
                AltoClef.INSTANCE.getInputControls().release(Input.MOVE_FORWARD);
            }
        }
        if (_unstuckTask != null && _unstuckTask.isActive() && !_unstuckTask.isFinished() && stuckInBlock(AltoClef.INSTANCE) != null) {
            setDebugState("Getting unstuck from block.");
            stuckCheck.reset();
            // Stop other tasks, we are JUST shimmying
            AltoClef.INSTANCE.getClientBaritone().getCustomGoalProcess().onLostControl();
            AltoClef.INSTANCE.getClientBaritone().getExploreProcess().onLostControl();
            return _unstuckTask;
        }
        if (!_moveChecker.check(AltoClef.INSTANCE) || !stuckCheck.check(AltoClef.INSTANCE)) {
            BlockPos blockStuck = stuckInBlock(AltoClef.INSTANCE);
            if (blockStuck != null) {
                _unstuckTask = getFenceUnstuckTask();
                return _unstuckTask;
            }
            stuckCheck.reset();
        }
        if (!_moveChecker.check(AltoClef.INSTANCE)) {
            _moveChecker.reset();
            AltoClef.INSTANCE.getBlockTracker().requestBlockUnreachable(_pos);
        }

        // do NOT break if we're standing above it and it's dangerous below...
        if (!WorldHelper.isSolid(AltoClef.INSTANCE, _pos.up()) && AltoClef.INSTANCE.getPlayer().getPos().y > _pos.getY() && _pos.isWithinDistance(AltoClef.INSTANCE.getPlayer().isOnGround() ? AltoClef.INSTANCE.getPlayer().getPos() : AltoClef.INSTANCE.getPlayer().getPos().add(0, -1, 0), 0.89)) {
            if (WorldHelper.dangerousToBreakIfRightAbove(AltoClef.INSTANCE, _pos)) {
                setDebugState("It's dangerous to break as we're right above it, moving away and trying again.");
                return new RunAwayFromPositionTask(3, _pos.getY(), _pos);
            }
        }

        // We're trying to mine
        Optional<Rotation> reach = LookHelper.getReach(_pos);
        if (reach.isPresent() && (AltoClef.INSTANCE.getPlayer().isTouchingWater() || AltoClef.INSTANCE.getPlayer().isOnGround()) &&
                !AltoClef.INSTANCE.getFoodChain().needsToEat() && !WorldHelper.isInNetherPortal(AltoClef.INSTANCE) &&
                AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
            setDebugState("Block in range, mining...");
            stuckCheck.reset();
            isMining = true;
            AltoClef.INSTANCE.getInputControls().release(Input.SNEAK);
            AltoClef.INSTANCE.getInputControls().release(Input.MOVE_BACK);
            AltoClef.INSTANCE.getInputControls().release(Input.MOVE_FORWARD);
            // Break the block, force it.
            AltoClef.INSTANCE.getClientBaritone().getCustomGoalProcess().onLostControl();
            AltoClef.INSTANCE.getClientBaritone().getBuilderProcess().onLostControl();
            if (!LookHelper.isLookingAt(AltoClef.INSTANCE, reach.get())) {
                reach.ifPresent(rotation -> LookHelper.lookAt(AltoClef.INSTANCE, rotation));
            }
            if (LookHelper.isLookingAt(AltoClef.INSTANCE, reach.get())) {
                // Tool equip is handled in `PlayerInteractionFixChain`. Oof.
                AltoClef.INSTANCE.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
            }
        } else {
            setDebugState("Getting to block...");
            if (isMining && AltoClef.INSTANCE.getPlayer().isTouchingWater()) {
                isMining = false;
                AltoClef.INSTANCE.getBlockTracker().requestBlockUnreachable(_pos);
            } else {
                isMining = false;
            }
            boolean isCloseToMoveBack = _pos.isWithinDistance(AltoClef.INSTANCE.getPlayer().getPos(), 2);
            if (isCloseToMoveBack) {
                if (!AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing() && !AltoClef.INSTANCE.getPlayer().isTouchingWater() &&
                        !AltoClef.INSTANCE.getFoodChain().needsToEat()) {
                    AltoClef.INSTANCE.getInputControls().hold(Input.SNEAK);
                    AltoClef.INSTANCE.getInputControls().hold(Input.MOVE_BACK);
                } else {
                    AltoClef.INSTANCE.getInputControls().release(Input.SNEAK);
                    AltoClef.INSTANCE.getInputControls().release(Input.MOVE_BACK);
                }
            }
            if (!AltoClef.INSTANCE.getClientBaritone().getCustomGoalProcess().isActive()) {
                AltoClef.INSTANCE.getClientBaritone().getBuilderProcess().onLostControl();
                AltoClef.INSTANCE.getClientBaritone().getCustomGoalProcess().setGoalAndPath(AltoClef.INSTANCE.getWorld().getBlockState(_pos.up()).getBlock() ==
                        Blocks.SNOW ? new GoalBlock(_pos) : new GoalNear(_pos, 1));
            }
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().forceCancel();
        if (!AltoClef.inGame()) {
            return;
        }
        // Do not keep breaking.
        // Can lead to trouble, for example, if lava is right above the NEXT block.
        AltoClef.INSTANCE.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, false);
        AltoClef.INSTANCE.getInputControls().release(Input.SNEAK);
        AltoClef.INSTANCE.getInputControls().release(Input.MOVE_BACK);
        AltoClef.INSTANCE.getInputControls().release(Input.MOVE_FORWARD);
    }

    @Override
    public boolean isFinished() {
        return WorldHelper.isAir(AltoClef.INSTANCE, _pos);//;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DestroyBlockTask task) {
            return task._pos.equals(_pos);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Destroy block at " + _pos.toShortString();
    }
}
