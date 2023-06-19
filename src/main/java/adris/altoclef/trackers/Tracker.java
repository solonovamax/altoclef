package adris.altoclef.trackers;

public abstract class Tracker {
    // Needs to update
    private boolean _dirty = true;

    public Tracker(TrackerManager manager) {
        manager.addTracker(this);
    }

    public void setDirty() {
        _dirty = true;
    }

    // Virtual
    protected boolean isDirty() {
        return _dirty;
    }

    protected void ensureUpdated() {
        if (isDirty()) {
            updateState();
            _dirty = false;
        }
    }

    protected abstract void updateState();

    protected abstract void reset();
}
