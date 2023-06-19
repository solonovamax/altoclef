package adris.altoclef.tasks.movement;

import adris.altoclef.Debug;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.input.Input;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.util.math.BlockPos;

/**
 * Turns a baritone goal into a task.
 */
public abstract class CustomBaritoneGoalTask extends Task implements ITaskRequiresGrounded {
    private final Task _wanderTask = new TimeoutWanderTask(5, true);
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final boolean _wander;
    protected MovementProgressChecker _checker = new MovementProgressChecker();
    protected Goal _cachedGoal = null;
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

    // This happens all the time in mineshafts and swamps/jungles

    public CustomBaritoneGoalTask(boolean wander) {
        _wander = wander;
    }

    public CustomBaritoneGoalTask() {
        this(true);
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
        _checker.reset();
        stuckCheck.reset();
    }

    @Override
    protected Task onTick() {
        if (AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
            _checker.reset();
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
        if (!_checker.check(AltoClef.INSTANCE) || !stuckCheck.check(AltoClef.INSTANCE)) {
            BlockPos blockStuck = stuckInBlock(AltoClef.INSTANCE);
            if (blockStuck != null) {
                _unstuckTask = getFenceUnstuckTask();
                return _unstuckTask;
            }
            stuckCheck.reset();
        }
        if (_cachedGoal == null) {
            _cachedGoal = newGoal(AltoClef.INSTANCE);
        }

        if (_wander) {
            if (isFinished()) {
                // Don't wander if we've reached our goal.
                _checker.reset();
            } else {
                if (_wanderTask.isActive() && !_wanderTask.isFinished()) {
                    setDebugState("Wandering...");
                    _checker.reset();
                    return _wanderTask;
                }
                if (!_checker.check(AltoClef.INSTANCE)) {
                    Debug.logMessage("Failed to make progress on goal, wandering.");
                    onWander(AltoClef.INSTANCE);
                    return _wanderTask;
                }
            }
        }

        if (!AltoClef.INSTANCE.getClientBaritone().getCustomGoalProcess().isActive()) {
            AltoClef.INSTANCE.getClientBaritone().getCustomGoalProcess().setGoalAndPath(_cachedGoal);
        }
        setDebugState("Completing goal.");
        return null;
    }

    @Override
    public boolean isFinished() {
        if (_cachedGoal == null) {
            _cachedGoal = newGoal(AltoClef.INSTANCE);
        }
        return _cachedGoal != null && _cachedGoal.isInGoal(AltoClef.INSTANCE.getPlayer().getBlockPos());
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().forceCancel();
    }

    protected abstract Goal newGoal(AltoClef mod);

    protected void onWander(AltoClef mod) {
    }
}
