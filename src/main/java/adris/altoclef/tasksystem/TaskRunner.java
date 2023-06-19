package adris.altoclef.tasksystem;

import gay.solonovamax.altoclef.AltoClef;
import adris.altoclef.Debug;

import java.util.ArrayList;

public class TaskRunner {

    private final ArrayList<TaskChain> _chains = new ArrayList<>();
    private boolean _active;

    private TaskChain _cachedCurrentTaskChain = null;

    public TaskRunner() {
        _active = false;
    }

    public void tick() {
        if (!_active || !AltoClef.inGame()) return;
        // Get highest priority chain and run
        TaskChain maxChain = null;
        float maxPriority = Float.NEGATIVE_INFINITY;
        for (TaskChain chain : _chains) {
            if (!chain.isActive()) continue;
            float priority = chain.getPriority();
            if (priority > maxPriority) {
                maxPriority = priority;
                maxChain = chain;
            }
        }
        if (_cachedCurrentTaskChain != null && maxChain != _cachedCurrentTaskChain) {
            _cachedCurrentTaskChain.onInterrupt(maxChain);
        }
        _cachedCurrentTaskChain = maxChain;
        if (maxChain != null) {
            maxChain.tick();
        }
    }

    public void addTaskChain(TaskChain chain) {
        _chains.add(chain);
    }

    public void enable() {
        if (!_active) {
            AltoClef.INSTANCE.getBehaviour().push();
            AltoClef.INSTANCE.getBehaviour().setPauseOnLostFocus(false);
        }
        _active = true;
    }

    public void disable() {
        if (_active) {
            AltoClef.INSTANCE.getBehaviour().pop();
        }
        for (TaskChain chain : _chains) {
            chain.stop();
        }
        _active = false;

        Debug.logMessage("Stopped");
    }

    public TaskChain getCurrentTaskChain() {
        return _cachedCurrentTaskChain;
    }

    // Kinda jank ngl
    public AltoClef getMod() {
        return AltoClef.INSTANCE;
    }
}
