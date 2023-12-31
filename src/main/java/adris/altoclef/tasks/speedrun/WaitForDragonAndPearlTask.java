package adris.altoclef.tasks.speedrun;

import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.entity.DoToClosestEntityTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasks.movement.GetToYTask;
import adris.altoclef.tasks.movement.RunAwayFromPositionTask;
import adris.altoclef.tasks.movement.ThrowEnderPearlSimpleProjectileTask;
import adris.altoclef.tasks.resources.GetBuildingMaterialsTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

// TODO:
// The 10 Portal pillars form a 43 block radius, but the angle offset/cycle is random.
// Have an internal "cycle" value or something to keep track of where that cycle is
// Detect that value by scrolling around the 43 block radius in search of obsidian and finding
// the "midpoint" between two spots of obsidian and anything else
// Then, when pillaring, make sure we move to one of those areas (so we can move further out without
// risking hitting an obsidian tower)
public class WaitForDragonAndPearlTask extends Task implements IDragonWaiter {
    // How far to travel away from the portal, in XZ
    private static final double XZ_RADIUS = 30;
    private static final double XZ_RADIUS_TOO_FAR = 38;
    // How high to pillar
    private static final int HEIGHT = 42; //Increase height because this too low

    private static final int CLOSE_ENOUGH_DISTANCE = 15;

    private static final double DRAGON_FIREBALL_TOO_CLOSE_RANGE = 40;
    private final Task _buildingMaterialsTask = new GetBuildingMaterialsTask(HEIGHT + 10);
    boolean inCenter;
    private Task _heightPillarTask;
    private Task _throwPearlTask;
    private BlockPos _targetToPearl;
    private boolean _dragonIsPerching;
    // To avoid dragons breath
    private Task _pillarUpFurther;

    @Override
    public void setExitPortalTop(BlockPos top) {
        BlockPos actualTarget = top.down();
        if (!actualTarget.equals(_targetToPearl)) {
            _targetToPearl = actualTarget;
            _throwPearlTask = new ThrowEnderPearlSimpleProjectileTask(actualTarget);
        }
    }

    @Override
    public void setPerchState(boolean perching) {
        _dragonIsPerching = perching;
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected Task onTick() {
        if (_throwPearlTask != null && _throwPearlTask.isActive() && !_throwPearlTask.isFinished()) {
            setDebugState("Throwing pearl!");
            return _throwPearlTask;
        }

        if (_pillarUpFurther != null && _pillarUpFurther.isActive() && !_pillarUpFurther.isFinished()) {
            setDebugState("PILLAR UP FURTHER to avoid dragon's breath");
            return _pillarUpFurther;
        }

        if (!AltoClef.INSTANCE.getItemStorage().hasItem(Items.ENDER_PEARL) && inCenter) {
            setDebugState("First get ender pearls.");
            return TaskCatalogue.getItemTask(Items.ENDER_PEARL, 1);
        }

        int minHeight = _targetToPearl.getY() + HEIGHT - 3;

        int deltaY = minHeight - AltoClef.INSTANCE.getPlayer().getBlockPos().getY();
        if (StorageHelper.getBuildingMaterialCount(AltoClef.INSTANCE) < Math.min(deltaY - 10, HEIGHT - 5) || _buildingMaterialsTask.isActive() && !_buildingMaterialsTask.isFinished()) {
            setDebugState("Collecting building materials...");
            return _buildingMaterialsTask;
        }

        // Our trigger to throw is that the dragon starts perching. We can be an arbitrary distance and we'll still do it lol
        if (_dragonIsPerching && LookHelper.cleanLineOfSight(AltoClef.INSTANCE.getPlayer(), _targetToPearl.up(), 300)) {
            Debug.logMessage("THROWING PEARL!!");
            return _throwPearlTask;
        }
        if (AltoClef.INSTANCE.getPlayer().getBlockPos().getY() < minHeight) {
            if (AltoClef.INSTANCE.getEntityTracker().entityFound(entity ->
                    AltoClef.INSTANCE.getPlayer().getPos().isInRange(entity.getPos(), 4), AreaEffectCloudEntity.class)) {
                if (AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() &&
                        !AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
                    LookHelper.lookAt(AltoClef.INSTANCE, AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
                }
                return null;
            }
            if (_heightPillarTask != null && _heightPillarTask.isActive() && !_heightPillarTask.isFinished()) {
                inCenter = true;
                if (AltoClef.INSTANCE.getEntityTracker().entityFound(EndCrystalEntity.class)) {
                    return new DoToClosestEntityTask(
                            (toDestroy) -> {
                                if (toDestroy.isInRange(AltoClef.INSTANCE.getPlayer(), 7)) {
                                    AltoClef.INSTANCE.getControllerExtras().attack(toDestroy);
                                }
                                if (AltoClef.INSTANCE.getPlayer().getBlockPos().getY() < minHeight) {
                                    return _heightPillarTask;
                                } else {
                                    if (AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() &&
                                            !AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
                                        LookHelper.lookAt(AltoClef.INSTANCE, AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
                                    }
                                    return null;
                                }
                            },
                            EndCrystalEntity.class
                    );
                }
                setDebugState("Pillaring up!");
                return _heightPillarTask;
            }
        } else {
            setDebugState("We're high enough.");
            // If a fireball is too close, run UP
            Optional<Entity> dragonFireball = AltoClef.INSTANCE.getEntityTracker().getClosestEntity(DragonFireballEntity.class);
            if (dragonFireball.isPresent() && dragonFireball.get().isInRange(AltoClef.INSTANCE.getPlayer(), DRAGON_FIREBALL_TOO_CLOSE_RANGE) && LookHelper.cleanLineOfSight(AltoClef.INSTANCE.getPlayer(), dragonFireball.get().getPos(), DRAGON_FIREBALL_TOO_CLOSE_RANGE)) {
                _pillarUpFurther = new GetToYTask(AltoClef.INSTANCE.getPlayer().getBlockY() + 5);
                Debug.logMessage("HOLDUP");
                return _pillarUpFurther;
            }
            if (AltoClef.INSTANCE.getEntityTracker().entityFound(EndCrystalEntity.class)) {
                return new DoToClosestEntityTask(
                        (toDestroy) -> {
                            if (toDestroy.isInRange(AltoClef.INSTANCE.getPlayer(), 7)) {
                                AltoClef.INSTANCE.getControllerExtras().attack(toDestroy);
                            }
                            if (AltoClef.INSTANCE.getPlayer().getBlockPos().getY() < minHeight) {
                                return _heightPillarTask;
                            } else {
                                if (AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() &&
                                        !AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
                                    LookHelper.lookAt(AltoClef.INSTANCE, AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
                                }
                                return null;
                            }
                        },
                        EndCrystalEntity.class
                );
            }
            if (AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() &&
                    !AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
                LookHelper.lookAt(AltoClef.INSTANCE, AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
            }
            return null;
        }
        if (WorldHelper.inRangeXZ(AltoClef.INSTANCE.getPlayer(), _targetToPearl, XZ_RADIUS)) {
            if (AltoClef.INSTANCE.getEntityTracker().entityFound(entity ->
                    AltoClef.INSTANCE.getPlayer().getPos().isInRange(entity.getPos(), 4), AreaEffectCloudEntity.class)) {
                if (AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() &&
                        !AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
                    LookHelper.lookAt(AltoClef.INSTANCE, AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
                }
                return null;
            }
            setDebugState("Moving away from center...");
            return new RunAwayFromPositionTask(XZ_RADIUS, _targetToPearl);
        }
        if (!WorldHelper.inRangeXZ(AltoClef.INSTANCE.getPlayer(), _targetToPearl, XZ_RADIUS_TOO_FAR)) {
            if (AltoClef.INSTANCE.getEntityTracker().entityFound(entity ->
                    AltoClef.INSTANCE.getPlayer().getPos().isInRange(entity.getPos(), 4), AreaEffectCloudEntity.class)) {
                if (AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() &&
                        !AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
                    LookHelper.lookAt(AltoClef.INSTANCE, AltoClef.INSTANCE.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
                }
                return null;
            }
            setDebugState("Moving in (too far, might hit pillars)");
            return new GetToXZTask(0, 0);
        }
        // We're far enough, pillar up!
        _heightPillarTask = new GetToYTask(minHeight);
        return _heightPillarTask;
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof WaitForDragonAndPearlTask;
    }

    @Override
    public boolean isFinished() {
        return _dragonIsPerching
                && ((_throwPearlTask == null || (_throwPearlTask.isActive() && _throwPearlTask.isFinished()))
                || WorldHelper.inRangeXZ(AltoClef.INSTANCE.getPlayer(), _targetToPearl, CLOSE_ENOUGH_DISTANCE));
    }

    @Override
    protected String toDebugString() {
        return "Waiting for Dragon Perch + Pearling";
    }
}
