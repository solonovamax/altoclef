package adris.altoclef.chains;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.MLGBucketTask;
import adris.altoclef.tasksystem.ITaskOverridesGrounded;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;

import java.util.Optional;

@SuppressWarnings("UnnecessaryLocalVariable")
public class MLGBucketFallChain extends SingleTaskChain implements ITaskOverridesGrounded {

    private final TimerGame _tryCollectWaterTimer = new TimerGame(4);
    private final TimerGame _pickupRepeatTimer = new TimerGame(0.25);
    private MLGBucketTask _lastMLG = null;
    private boolean _wasPickingUp = false;
    private boolean _doingChorusFruit = false;

    public MLGBucketFallChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish() {
        //_lastMLG = null;
    }

    @Override
    public float getPriority() {
        if (!AltoClef.inGame()) return Float.NEGATIVE_INFINITY;
        if (isFallingOhNo(AltoClef.INSTANCE)) {
            _tryCollectWaterTimer.reset();
            setTask(new MLGBucketTask());
            _lastMLG = (MLGBucketTask) _mainTask;
            return 100;
        } else if (!_tryCollectWaterTimer.elapsed()) { // Why -0.5? Cause it's slower than -0.7.
            // We just placed water, try to collect it.
            if (AltoClef.INSTANCE.getItemStorage().hasItem(Items.BUCKET) && !AltoClef.INSTANCE.getItemStorage().hasItem(Items.WATER_BUCKET)) {
                if (_lastMLG != null) {
                    BlockPos placed = _lastMLG.getWaterPlacedPos();
                    boolean isPlacedWater;
                    try {
                        isPlacedWater = AltoClef.INSTANCE.getWorld().getBlockState(placed).getBlock() == Blocks.WATER;
                    } catch (Exception e) {
                        isPlacedWater = false;
                    }
                    // Debug.logInternal("PLACED: " + placed);
                    if (placed != null && placed.isWithinDistance(AltoClef.INSTANCE.getPlayer().getPos(), 5.5) && isPlacedWater) {
                        BlockPos toInteract = placed;
                        // Allow looking at fluids
                        AltoClef.INSTANCE.getBehaviour().push();
                        AltoClef.INSTANCE.getBehaviour().setRayTracingFluidHandling(RaycastContext.FluidHandling.SOURCE_ONLY);
                        Optional<Rotation> reach = LookHelper.getReach(toInteract, Direction.UP);
                        if (reach.isPresent()) {
                            AltoClef.INSTANCE.getClientBaritone().getLookBehavior().updateTarget(reach.get(), true);
                            if (AltoClef.INSTANCE.getClientBaritone().getPlayerContext().isLookingAt(toInteract)) {
                                if (AltoClef.INSTANCE.getSlotHandler().forceEquipItem(Items.BUCKET)) {
                                    if (_pickupRepeatTimer.elapsed()) {
                                        // Pick up
                                        _pickupRepeatTimer.reset();
                                        AltoClef.INSTANCE.getInputControls().tryPress(Input.CLICK_RIGHT);
                                        _wasPickingUp = true;
                                    } else if (_wasPickingUp) {
                                        // Stop picking up, wait and try again.
                                        _wasPickingUp = false;
                                    }
                                }
                            }
                        } else {
                            // Eh just try collecting water the regular way if all else fails.
                            setTask(TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1));
                        }
                        AltoClef.INSTANCE.getBehaviour().pop();
                        return 60;
                    }
                }
            }
        }
        if (_wasPickingUp) {
            _wasPickingUp = false;
            _lastMLG = null;
        }
        if (AltoClef.INSTANCE.getPlayer().hasStatusEffect(StatusEffects.LEVITATION) &&
                !AltoClef.INSTANCE.getPlayer().getItemCooldownManager().isCoolingDown(Items.CHORUS_FRUIT) &&
                AltoClef.INSTANCE.getPlayer().getActiveStatusEffects().get(StatusEffects.LEVITATION).getDuration() <= 70 &&
                AltoClef.INSTANCE.getItemStorage().hasItemInventoryOnly(Items.CHORUS_FRUIT) &&
                !AltoClef.INSTANCE.getItemStorage().hasItemInventoryOnly(Items.WATER_BUCKET)) {
            _doingChorusFruit = true;
            AltoClef.INSTANCE.getSlotHandler().forceEquipItem(Items.CHORUS_FRUIT);
            AltoClef.INSTANCE.getInputControls().hold(Input.CLICK_RIGHT);
            AltoClef.INSTANCE.getExtraBaritoneSettings().setInteractionPaused(true);
        } else if (_doingChorusFruit) {
            _doingChorusFruit = false;
            AltoClef.INSTANCE.getInputControls().release(Input.CLICK_RIGHT);
            AltoClef.INSTANCE.getExtraBaritoneSettings().setInteractionPaused(false);
        }
        _lastMLG = null;
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public String getName() {
        return "MLG Water Bucket Fall Chain";
    }

    @Override
    public boolean isActive() {
        // We're always checking for mlg.
        return true;
    }

    public boolean doneMLG() {
        return _lastMLG == null;
    }

    public boolean isChorusFruiting() {
        return _doingChorusFruit;
    }

    public boolean isFallingOhNo(AltoClef mod) {
        if (!mod.getModSettings().shouldAutoMLGBucket()) {
            return false;
        }
        if (mod.getPlayer().isSwimming() || mod.getPlayer().isTouchingWater() || mod.getPlayer().isOnGround() || mod.getPlayer().isClimbing()) {
            // We're grounded.
            return false;
        }
        double ySpeed = mod.getPlayer().getVelocity().y;
        return ySpeed < -0.7;
    }
}
