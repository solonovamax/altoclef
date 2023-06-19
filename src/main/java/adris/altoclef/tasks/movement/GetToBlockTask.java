package adris.altoclef.tasks.movement;

import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;

public class GetToBlockTask extends CustomBaritoneGoalTask implements ITaskRequiresGrounded {

    private final BlockPos _position;
    private final boolean _preferStairs;
    private final Dimension _dimension;

    public GetToBlockTask(BlockPos position, boolean preferStairs) {
        this(position, preferStairs, null);
    }

    public GetToBlockTask(BlockPos position, Dimension dimension) {
        this(position, false, dimension);
    }

    public GetToBlockTask(BlockPos position, boolean preferStairs, Dimension dimension) {
        _dimension = dimension;
        _position = position;
        _preferStairs = preferStairs;
    }

    public GetToBlockTask(BlockPos position) {
        this(position, false);
    }

    @Override
    protected Task onTick() {
        if (_dimension != null && WorldHelper.getCurrentDimension() != _dimension) {
            return new DefaultGoToDimensionTask(_dimension);
        }
        return super.onTick();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (_preferStairs) {
            AltoClef.INSTANCE.getBehaviour().push();
            AltoClef.INSTANCE.getBehaviour().setPreferredStairs(true);
        }
    }


    @Override
    protected void onStop(Task interruptTask) {
        super.onStop(interruptTask);
        if (_preferStairs) {
            AltoClef.INSTANCE.getBehaviour().pop();
        }
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GetToBlockTask task) {
            return task._position.equals(_position) && task._preferStairs == _preferStairs && task._dimension == _dimension;
        }
        return false;
    }

    @Override
    public boolean isFinished() {
        return super.isFinished() && (_dimension == null || _dimension == WorldHelper.getCurrentDimension());
    }

    @Override
    protected String toDebugString() {
        return "Getting to block " + _position + (_dimension != null ? " in dimension " + _dimension : "");
    }


    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalBlock(_position);
    }

    @Override
    protected void onWander(AltoClef mod) {
        super.onWander(mod);
        mod.getBlockTracker().requestBlockUnreachable(_position);
    }
}
