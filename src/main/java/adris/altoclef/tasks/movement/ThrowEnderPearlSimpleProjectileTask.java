package adris.altoclef.tasks.movement;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.ProjectileHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ThrowEnderPearlSimpleProjectileTask extends Task {

    private final TimerGame _thrownTimer = new TimerGame(5);
    private final BlockPos _target;

    private boolean _thrown = false;

    public ThrowEnderPearlSimpleProjectileTask(BlockPos target) {
        _target = target;
    }

    private static boolean cleanThrow(AltoClef mod, float yaw, float pitch) {
        Rotation rotation = new Rotation(yaw, -1 * pitch);
        float range = 3f;
        Vec3d delta = LookHelper.toVec3d(rotation).multiply(range);
        Vec3d start = LookHelper.getCameraPos(mod);
        return LookHelper.cleanLineOfSight(start.add(delta), range);
    }

    private static Rotation calculateThrowLook(AltoClef mod, BlockPos end) {
        Vec3d start = ProjectileHelper.getThrowOrigin(mod.getPlayer());
        Vec3d endCenter = WorldHelper.toVec3d(end);
        double gravity = ProjectileHelper.THROWN_ENTITY_GRAVITY_ACCEL;
        double speed = 1.5;
        float yaw = LookHelper.getLookRotation(mod, end).getYaw();
        double flatDistance = WorldHelper.distanceXZ(start, endCenter);
        double[] pitches = ProjectileHelper.calculateAnglesForSimpleProjectileMotion(start.y - endCenter.y, flatDistance, speed, gravity);
        double pitch = cleanThrow(mod, yaw, (float) pitches[0]) ? pitches[0] : pitches[1];
        return new Rotation(yaw, -1 * (float) pitch);
    }

    @Override
    protected void onStart() {
        _thrownTimer.forceElapse();
        _thrown = false;
    }

    @Override
    protected Task onTick() {
        // TODO: Unlikely/minor nitpick, but there could be other people throwing ender pearls, which would delay the bot.
        if (AltoClef.INSTANCE.getEntityTracker().entityFound(EnderPearlEntity.class)) {
            _thrownTimer.reset();
        }
        if (_thrownTimer.elapsed()) {
            if (AltoClef.INSTANCE.getSlotHandler().forceEquipItem(Items.ENDER_PEARL)) {
                Rotation lookTarget = calculateThrowLook(AltoClef.INSTANCE, _target);
                LookHelper.lookAt(AltoClef.INSTANCE, lookTarget);
                if (LookHelper.isLookingAt(AltoClef.INSTANCE, lookTarget)) {
                    AltoClef.INSTANCE.getInputControls().tryPress(Input.CLICK_RIGHT);
                    _thrown = true;
                    _thrownTimer.reset();
                }
            }
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    public boolean isFinished() {
        return _thrown && _thrownTimer.elapsed() || (!_thrown && !AltoClef.INSTANCE.getItemStorage().hasItem(Items.ENDER_PEARL));
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ThrowEnderPearlSimpleProjectileTask task) {
            return task._target.equals(_target);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Simple Ender Pearling to " + _target;
    }
}
