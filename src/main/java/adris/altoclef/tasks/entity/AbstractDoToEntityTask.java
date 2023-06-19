package adris.altoclef.tasks.entity;

import adris.altoclef.Debug;
import adris.altoclef.tasks.movement.GetToEntityTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.Slot;
import baritone.api.pathing.goals.GoalRunAway;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Optional;

/**
 * Interacts with an entity while maintaining distance.
 * <p>
 * The interaction is abstract.
 */
public abstract class AbstractDoToEntityTask extends Task implements ITaskRequiresGrounded {
    protected final MovementProgressChecker _progress = new MovementProgressChecker();
    private final double _maintainDistance;
    private final double _combatGuardLowerRange;
    private final double _combatGuardLowerFieldRadius;

    public AbstractDoToEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        _maintainDistance = maintainDistance;
        _combatGuardLowerRange = combatGuardLowerRange;
        _combatGuardLowerFieldRadius = combatGuardLowerFieldRadius;
    }

    public AbstractDoToEntityTask(double maintainDistance) {
        this(maintainDistance, 0, Double.POSITIVE_INFINITY);
    }

    public AbstractDoToEntityTask(double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        this(-1, combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    @Override
    protected void onStart() {
        _progress.reset();
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> AltoClef.INSTANCE.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            if (ItemHelper.canThrowAwayStack(AltoClef.INSTANCE, cursorStack)) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(AltoClef.INSTANCE);
            // Try throwing away cursor slot if it's garbage
            garbage.ifPresent(slot -> AltoClef.INSTANCE.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        } // Kinda duct tape but it should be future proof ish
    }

    @Override
    protected Task onTick() {
        if (AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing()) {
            _progress.reset();
        }

        Optional<Entity> checkEntity = getEntityTarget(AltoClef.INSTANCE);


        // Oof
        if (checkEntity.isEmpty()) {
            AltoClef.INSTANCE.getMobDefenseChain().resetTargetEntity();
            AltoClef.INSTANCE.getMobDefenseChain().resetForceField();
        } else {
            AltoClef.INSTANCE.getMobDefenseChain().setTargetEntity(checkEntity.get());
        }
        if (checkEntity.isPresent()) {
            Entity entity = checkEntity.get();

            double playerReach = AltoClef.INSTANCE.getModSettings().getEntityReachRange();

            // TODO: This is basically useless.
            EntityHitResult result = LookHelper.raycast(AltoClef.INSTANCE.getPlayer(), entity, playerReach);

            double sqDist = entity.squaredDistanceTo(AltoClef.INSTANCE.getPlayer());

            if (sqDist < _combatGuardLowerRange * _combatGuardLowerRange) {
                AltoClef.INSTANCE.getMobDefenseChain().setForceFieldRange(_combatGuardLowerFieldRadius);
            } else {
                AltoClef.INSTANCE.getMobDefenseChain().resetForceField();
            }

            // If we don't specify a maintain distance, default to within 1 block of our reach.
            double maintainDistance = _maintainDistance >= 0 ? _maintainDistance : playerReach - 1;

            boolean tooClose = sqDist < maintainDistance * maintainDistance;

            // Step away if we're too close
            if (tooClose) {
                // setDebugState("Maintaining distance");
                if (!AltoClef.INSTANCE.getClientBaritone().getCustomGoalProcess().isActive()) {
                    AltoClef.INSTANCE.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalRunAway(maintainDistance, entity.getBlockPos()));
                }
            }

            if (AltoClef.INSTANCE.getControllerExtras().inRange(entity) && result != null &&
                    result.getType() == HitResult.Type.ENTITY && !AltoClef.INSTANCE.getFoodChain().needsToEat() &&
                    !AltoClef.INSTANCE.getMLGBucketChain().isFallingOhNo(AltoClef.INSTANCE) && AltoClef.INSTANCE.getMLGBucketChain().doneMLG() &&
                    !AltoClef.INSTANCE.getMLGBucketChain().isChorusFruiting() &&
                    AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
                _progress.reset();
                return onEntityInteract(AltoClef.INSTANCE, entity);
            } else if (!tooClose) {
                setDebugState("Approaching target");
                if (!_progress.check(AltoClef.INSTANCE)) {
                    _progress.reset();
                    Debug.logMessage("Failed to get to target, blacklisting.");
                    AltoClef.INSTANCE.getEntityTracker().requestEntityUnreachable(entity);
                }
                // Move to target
                return new GetToEntityTask(entity, maintainDistance);
            }
        }
        return new TimeoutWanderTask();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof AbstractDoToEntityTask task) {
            if (!doubleCheck(task._maintainDistance, _maintainDistance)) return false;
            if (!doubleCheck(task._combatGuardLowerFieldRadius, _combatGuardLowerFieldRadius)) return false;
            if (!doubleCheck(task._combatGuardLowerRange, _combatGuardLowerRange)) return false;
            return isSubEqual(task);
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean doubleCheck(double a, double b) {
        if (Double.isInfinite(a) == Double.isInfinite(b)) return true;
        return Math.abs(a - b) < 0.1;
    }

    protected abstract boolean isSubEqual(AbstractDoToEntityTask other);

    protected abstract Task onEntityInteract(AltoClef mod, Entity entity);

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getMobDefenseChain().setTargetEntity(null);
        AltoClef.INSTANCE.getMobDefenseChain().resetForceField();
    }

    protected abstract Optional<Entity> getEntityTarget(AltoClef mod);

}
