package adris.altoclef.tasks.movement;

import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalFollowEntity;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.utils.input.Input;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class GetToEntityTask extends Task implements ITaskRequiresGrounded {
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final MovementProgressChecker _progress = new MovementProgressChecker();
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(5);
    private final Entity _entity;
    private final double _closeEnoughDistance;
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

    public GetToEntityTask(Entity entity, double closeEnoughDistance) {
        _entity = entity;
        _closeEnoughDistance = closeEnoughDistance;
    }

    public GetToEntityTask(Entity entity) {
        this(entity, 1);
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
        if (annoyingBlocks != null) {
            for (Block AnnoyingBlocks : annoyingBlocks) {
                return mod.getWorld().getBlockState(pos).getBlock() == AnnoyingBlocks ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof DoorBlock ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof FenceGateBlock ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof FlowerBlock;
            }
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
        _progress.reset();
        stuckCheck.reset();
        _wanderTask.resetWander();
    }

    @Override
    protected Task onTick() {
        if (AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
            _progress.reset();
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
        if (!_progress.check(AltoClef.INSTANCE) || !stuckCheck.check(AltoClef.INSTANCE)) {
            BlockPos blockStuck = stuckInBlock(AltoClef.INSTANCE);
            if (blockStuck != null) {
                _unstuckTask = getFenceUnstuckTask();
                return _unstuckTask;
            }
            stuckCheck.reset();
        }
        if (_wanderTask.isActive() && !_wanderTask.isFinished()) {
            _progress.reset();
            setDebugState("Failed to get to target, wandering for a bit.");
            return _wanderTask;
        }

        if (!AltoClef.INSTANCE.getClientBaritone().getCustomGoalProcess().isActive()) {
            AltoClef.INSTANCE.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalFollowEntity(_entity, _closeEnoughDistance));
        }

        if (AltoClef.INSTANCE.getPlayer().isInRange(_entity, _closeEnoughDistance)) {
            _progress.reset();
        }

        if (!_progress.check(AltoClef.INSTANCE)) {
            return _wanderTask;
        }

        setDebugState("Going to entity");
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().forceCancel();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GetToEntityTask task) {
            return task._entity.equals(_entity) && Math.abs(task._closeEnoughDistance - _closeEnoughDistance) < 0.1;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Approach entity " + _entity.getType().getTranslationKey();
    }
}
