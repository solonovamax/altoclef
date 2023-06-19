package adris.altoclef.tasks.movement;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;

public class GetToXZTask extends CustomBaritoneGoalTask {

    private final int _x, _z;
    private final Dimension _dimension;

    public GetToXZTask(int x, int z) {
        this(x, z, null);
    }

    public GetToXZTask(int x, int z, Dimension dimension) {
        _x = x;
        _z = z;
        _dimension = dimension;
    }

    @Override
    protected Task onTick() {
        if (_dimension != null && WorldHelper.getCurrentDimension() != _dimension) {
            return new DefaultGoToDimensionTask(_dimension);
        }
        return super.onTick();
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalXZ(_x, _z);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GetToXZTask task) {
            return task._x == _x && task._z == _z && task._dimension == _dimension;
        }
        return false;
    }

    @Override
    public boolean isFinished() {
        BlockPos cur = AltoClef.INSTANCE.getPlayer().getBlockPos();
        return (cur.getX() == _x && cur.getZ() == _z && (_dimension == null || _dimension == WorldHelper.getCurrentDimension()));
    }

    @Override
    protected String toDebugString() {
        return "Getting to (" + _x + "," + _z + ")" + (_dimension != null ? " in dimension " + _dimension : "");
    }
}
