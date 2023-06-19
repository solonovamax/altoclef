package adris.altoclef.tasks.misc;

import adris.altoclef.tasksystem.Task;
import gay.solonovamax.altoclef.AltoClef;

public class SleepThroughNightTask extends Task {

    @Override
    protected void onStart() {

    }

    @Override
    protected Task onTick() {
        return new PlaceBedAndSetSpawnTask().stayInBed();
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof SleepThroughNightTask;
    }

    @Override
    protected String toDebugString() {
        return "Sleeping through the night";
    }

    @Override
    public boolean isFinished() {
        // We're in daytime
        int time = (int) (AltoClef.INSTANCE.getWorld().getTimeOfDay() % 24000);
        return 0 <= time && time < 13000;
    }
}
