package adris.altoclef.tasks.movement;

import adris.altoclef.Playground;
import adris.altoclef.tasksystem.Task;
import gay.solonovamax.altoclef.AltoClef;

/**
 * Do nothing.
 */
public class IdleTask extends Task {
    @Override
    protected void onStart() {

    }

    @Override
    protected Task onTick() {
        // Do nothing except maybe test code
        Playground.IDLE_TEST_TICK_FUNCTION(AltoClef.INSTANCE);
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    public boolean isFinished() {
        // Never finish
        return false;
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof IdleTask;
    }

    @Override
    protected String toDebugString() {
        return "Idle";
    }
}
