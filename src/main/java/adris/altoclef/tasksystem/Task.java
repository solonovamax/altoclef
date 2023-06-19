package adris.altoclef.tasksystem;

import adris.altoclef.Debug;
import adris.altoclef.tasks.movement.TimeoutWanderTask;

import java.util.function.Predicate;

public abstract class Task {

    private String _oldDebugState = "";
    private String _debugState = "";

    private Task _sub = null;

    private boolean _first = true;

    private boolean _stopped = false;

    private boolean _active = false;

    public void tick(TaskChain parentChain) {
        parentChain.addTaskToChain(this);
        if (_first) {
            Debug.logInternal("Task START: " + this);
            _active = true;
            onStart();
            _first = false;
            _stopped = false;
        }
        if (_stopped) return;

        Task newSub = onTick();
        // Debug state print
        if (!_oldDebugState.equals(_debugState)) {
            Debug.logInternal(toString());
            _oldDebugState = _debugState;
        }
        // We have a sub task
        if (newSub != null) {
            if (!newSub.isEqual(_sub)) {
                if (canBeInterrupted(_sub, newSub)) {
                    // Our sub task is new
                    if (_sub != null) {
                        // Our previous sub must be interrupted.
                        _sub.stop(newSub);
                    }

                    _sub = newSub;
                }
            }

            // Run our child
            _sub.tick(parentChain);
        } else {
            // We are null
            if (_sub != null && canBeInterrupted(_sub, null)) {
                // Our previous sub must be interrupted.
                _sub.stop();
                _sub = null;
            }
        }
    }

    public void reset() {
        _first = true;
        _active = false;
        _stopped = false;
    }

    public void stop() {
        stop(null);
    }

    /**
     * Stops the task. Next time it's run it will run `onStart`
     */
    public void stop(Task interruptTask) {
        if (!_active) return;
        Debug.logInternal("Task STOP: " + this + ", interrupted by " + interruptTask);
        if (!_first) {
            onStop(interruptTask);
        }

        if (_sub != null && !_sub.stopped()) {
            _sub.stop(interruptTask);
        }

        _first = true;
        _active = false;
        _stopped = true;
    }

    /**
     * Lets the task know it's execution has been "suspended"
     * <p>
     * STILL RUNS `onStop`
     * <p>
     * Doesn't stop it all-together (meaning `isActive` still returns true)
     */
    public void interrupt(Task interruptTask) {
        if (!_active) return;
        if (!_first) {
            onStop(interruptTask);
        }

        if (_sub != null && !_sub.stopped()) {
            _sub.interrupt(interruptTask);
        }

        _first = true;
    }

    protected void setDebugState(String state) {
        if (state == null) {
            state = "";
        }
        _debugState = state;
    }

    // Virtual
    public boolean isFinished() {
        return false;
    }

    public boolean isActive() {
        return _active;
    }

    public boolean stopped() {
        return _stopped;
    }

    protected abstract void onStart();

    protected abstract Task onTick();

    // interruptTask = null if the task stopped cleanly
    protected abstract void onStop(Task interruptTask);

    protected abstract boolean isEqual(Task other);

    protected abstract String toDebugString();

    @Override
    public String toString() {
        return "<" + toDebugString() + "> " + _debugState;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Task task) {
            return isEqual(task);
        }
        return false;
    }

    public boolean thisOrChildSatisfies(Predicate<Task> pred) {
        Task t = this;
        while (t != null) {
            if (pred.test(t)) return true;
            t = t._sub;
        }
        return false;
    }

    public boolean thisOrChildAreTimedOut() {
        return thisOrChildSatisfies(task -> task instanceof TimeoutWanderTask);
    }

    /**
     * Sometimes a task just can NOT be bothered to be interrupted right now.
     * For instance, if we're in mid air and MUST complete the parkour movement.
     */
    private boolean canBeInterrupted(Task subTask, Task toInterruptWith) {
        if (subTask == null) return true;
        // Our task can declare that is FORCES itself to be active NOW.
        return (subTask.thisOrChildSatisfies(task -> {
            if (task instanceof ITaskCanForce canForce) {
                return !canForce.shouldForce(toInterruptWith);
            }
            return true;
        }));
    }
}
