package adris.altoclef.chains;

import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.time.Stopwatch;
import gay.solonovamax.altoclef.AltoClef;

public abstract class SingleTaskChain extends TaskChain {
    private final Stopwatch _taskStopwatch = new Stopwatch();
    protected Task _mainTask = null;
    private boolean _interrupted = false;

    private AltoClef _mod;

    public SingleTaskChain(TaskRunner runner) {
        super(runner);
        _mod = runner.getMod();
    }

    @Override
    protected void onTick() {
        if (!isActive()) return;

        if (_interrupted) {
            _interrupted = false;
            if (_mainTask != null) {
                _mainTask.reset();
            }
        }

        if (_mainTask != null) {
            if ((_mainTask.isFinished()) || _mainTask.stopped()) {
                onTaskFinish();
            } else {
                _mainTask.tick(this);
            }
        }
    }

    protected void onStop() {
        if (isActive() && _mainTask != null) {
            _mainTask.stop();
            _mainTask = null;
        }
    }

    public void setTask(Task task) {
        if (_mainTask == null || !_mainTask.equals(task)) {
            if (_mainTask != null) {
                _mainTask.stop(task);
            }
            _mainTask = task;
            if (task != null) task.reset();
        }
    }


    @Override
    public boolean isActive() {
        return _mainTask != null;
    }

    protected abstract void onTaskFinish();

    @Override
    public void onInterrupt(TaskChain other) {
        if (other != null) {
            Debug.logInternal("Chain Interrupted: " + this + " by " + other);
        }
        // Stop our task. When we're started up again, let our task know we need to run.
        _interrupted = true;
        if (_mainTask != null && _mainTask.isActive()) {
            _mainTask.interrupt(null);
        }
    }

    protected boolean isCurrentlyRunning(AltoClef mod) {
        return !_interrupted && _mainTask.isActive() && !_mainTask.isFinished();
    }

    public Task getCurrentTask() {
        return _mainTask;
    }
}
