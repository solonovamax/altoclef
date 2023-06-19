package adris.altoclef.tasksystem;

import java.util.ArrayList;
import java.util.List;

public abstract class TaskChain {

    private final List<Task> _cachedTaskChain = new ArrayList<>();

    public TaskChain(TaskRunner runner) {
        runner.addTaskChain(this);
    }

    public void tick() {
        _cachedTaskChain.clear();
        onTick();
    }

    public void stop() {
        _cachedTaskChain.clear();
        onStop();
    }

    protected abstract void onStop();

    public abstract void onInterrupt(TaskChain other);

    protected abstract void onTick();

    public abstract float getPriority();

    public abstract boolean isActive();

    public abstract String getName();

    public List<Task> getTasks() {
        return _cachedTaskChain;
    }

    void addTaskToChain(Task task) {
        _cachedTaskChain.add(task);
    }

    public String toString() {
        return getName();
    }

}
