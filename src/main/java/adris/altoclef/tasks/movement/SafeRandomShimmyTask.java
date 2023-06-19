package adris.altoclef.tasks.movement;

import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import gay.solonovamax.altoclef.AltoClef;

/**
 * Will move around randomly while holding shift
 * Used to escape weird situations where baritone doesn't work.
 */
public class SafeRandomShimmyTask extends Task {

    private final TimerGame _lookTimer;

    public SafeRandomShimmyTask(float randomLookInterval) {
        _lookTimer = new TimerGame(randomLookInterval);
    }

    public SafeRandomShimmyTask() {
        this(5);
    }

    @Override
    protected void onStart() {
        _lookTimer.reset();
    }

    @Override
    protected Task onTick() {
        if (_lookTimer.elapsed()) {
            Debug.logMessage("Random Orientation");
            _lookTimer.reset();
            LookHelper.randomOrientation(AltoClef.INSTANCE);
        }

        AltoClef.INSTANCE.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
        AltoClef.INSTANCE.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
        AltoClef.INSTANCE.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, false);
        AltoClef.INSTANCE.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.SNEAK, false);
        AltoClef.INSTANCE.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, false);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof SafeRandomShimmyTask;
    }

    @Override
    protected String toDebugString() {
        return "Shimmying";
    }
}
