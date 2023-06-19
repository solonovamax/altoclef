package adris.altoclef.trackers;

import gay.solonovamax.altoclef.AltoClef;

import java.util.ArrayList;

public class TrackerManager {

    private final ArrayList<Tracker> _trackers = new ArrayList<>();

    private boolean _wasInGame = false;

    public void tick() {
        boolean inGame = AltoClef.inGame();
        if (!inGame && _wasInGame) {
            // Reset when we leave our world
            for (Tracker tracker : _trackers) {
                tracker.reset();
            }
            // This is a a spaghetti. Fix at some point.
            AltoClef.INSTANCE.getChunkTracker().reset(AltoClef.INSTANCE);
            AltoClef.INSTANCE.getMiscBlockTracker().reset();
        }
        _wasInGame = inGame;

        for (Tracker tracker : _trackers) {
            tracker.setDirty();
        }
    }

    public void addTracker(Tracker tracker) {
        _trackers.add(tracker);
    }
}
