package adris.altoclef.control;

import adris.altoclef.Settings;
import adris.altoclef.chains.FoodChain;
import adris.altoclef.chains.MLGBucketFallChain;
import adris.altoclef.trackers.EntityTracker;
import adris.altoclef.trackers.storage.ItemStorageTracker;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.Baritone;
import baritone.altoclef.AltoClefSettings;
import baritone.api.utils.input.Input;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controls and applies killaura
 */
public class KillAura {
    // Smart aura data
    private final List<Entity> _targets = new ArrayList<>();
    private final TimerGame _hitDelay = new TimerGame(0.2);
    boolean _shielding = false;
    private double _forceFieldRange = Double.POSITIVE_INFINITY;
    private Entity _forceHit = null;

    public static void equipWeapon() {
        List<ItemStack> invStacks = AltoClef.INSTANCE.getItemStorage().getItemStacksPlayerInventory(true);
        if (!invStacks.isEmpty()) {
            float handDamage = Float.NEGATIVE_INFINITY;
            for (ItemStack invStack : invStacks) {
                if (invStack.getItem() instanceof SwordItem item) {
                    float itemDamage = item.getMaterial().getAttackDamage();
                    Item handItem = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem();
                    if (handItem instanceof SwordItem handToolItem) {
                        handDamage = handToolItem.getMaterial().getAttackDamage();
                    }
                    SlotHandler slotHandler = AltoClef.INSTANCE.getSlotHandler();
                    if (itemDamage > handDamage) {
                        slotHandler.forceEquipItem(item);
                    } else {
                        slotHandler.forceEquipItem(handItem);
                    }
                }
            }
        }
    }

    public void tickStart() {
        _targets.clear();
        _forceHit = null;
    }

    public void applyAura(Entity entity) {
        _targets.add(entity);
        // Always hit ghast balls.
        if (entity instanceof FireballEntity) _forceHit = entity;
    }

    public void setRange(double range) {
        _forceFieldRange = range;
    }

    public void tickEnd() {
        PlayerSlot offhandSlot = PlayerSlot.OFFHAND_SLOT;
        Item offhandItem = StorageHelper.getItemStackInSlot(offhandSlot).getItem();

        ClientPlayerEntity player = AltoClef.INSTANCE.getPlayer();

        MLGBucketFallChain mlgBucketChain = AltoClef.INSTANCE.getMLGBucketChain();
        ItemStorageTracker itemStorage = AltoClef.INSTANCE.getItemStorage();
        EntityTracker entityTracker = AltoClef.INSTANCE.getEntityTracker();
        FoodChain foodChain = AltoClef.INSTANCE.getFoodChain();
        SlotHandler slotHandler = AltoClef.INSTANCE.getSlotHandler();
        Settings modSettings = AltoClef.INSTANCE.getModSettings();

        Optional<Entity> entities = _targets.stream().min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(player)));

        if (entities.isPresent() && player.getHealth() >= 10 &&
                !entityTracker.entityFound(PotionEntity.class) && !foodChain.needsToEat() &&
                (itemStorage.hasItem(Items.SHIELD) || itemStorage.hasItemInOffhand(Items.SHIELD)) &&
                (Double.isInfinite(_forceFieldRange) || entities.get().squaredDistanceTo(player) < _forceFieldRange * _forceFieldRange ||
                        entities.get().squaredDistanceTo(player) < 40) &&
                !mlgBucketChain.isFallingOhNo(AltoClef.INSTANCE) && mlgBucketChain.doneMLG() &&
                !mlgBucketChain.isChorusFruiting() &&
                !player.getItemCooldownManager().isCoolingDown(offhandItem)) {
            if (entities.get().getClass() != CreeperEntity.class && entities.get().getClass() != HoglinEntity.class &&
                    entities.get().getClass() != ZoglinEntity.class && entities.get().getClass() != WardenEntity.class &&
                    entities.get().getClass() != WitherEntity.class) {
                LookHelper.lookAt(AltoClef.INSTANCE, entities.get().getEyePos());
                ItemStack shieldSlot = StorageHelper.getItemStackInSlot(PlayerSlot.OFFHAND_SLOT);
                if (shieldSlot.getItem() != Items.SHIELD) {
                    slotHandler.forceEquipItemToOffhand(Items.SHIELD);
                } else {
                    startShielding();
                    performDelayedAttack(AltoClef.INSTANCE);
                    return;
                }
            }
        } else {
            stopShielding();
        }
        // Run force field on map

        switch (modSettings.getForceFieldStrategy()) {
            case FASTEST:
                performFastestAttack(AltoClef.INSTANCE);
                break;
            case SMART:
                if (_targets.size() <= 2 || _targets.stream().allMatch(entity -> entity instanceof SkeletonEntity) ||
                        _targets.stream().allMatch(entity -> entity instanceof WitchEntity) ||
                        _targets.stream().allMatch(entity -> entity instanceof PillagerEntity) ||
                        _targets.stream().allMatch(entity -> entity instanceof PiglinEntity) ||
                        _targets.stream().allMatch(entity -> entity instanceof StrayEntity) ||
                        _targets.stream().allMatch(entity -> entity instanceof BlazeEntity)) {
                    performDelayedAttack(AltoClef.INSTANCE);
                } else {
                    if (!foodChain.needsToEat() && !mlgBucketChain.isFallingOhNo(AltoClef.INSTANCE) &&
                            mlgBucketChain.doneMLG() && !mlgBucketChain.isChorusFruiting()) {
                        // Attack force mobs ALWAYS.
                        if (_forceHit != null) {
                            attack(_forceHit, true);
                        }
                        if (_hitDelay.elapsed()) {
                            _hitDelay.reset();

                            Optional<Entity> toHit = _targets.stream().min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(player)));

                            toHit.ifPresent(entity -> attack(entity, true));
                        }
                    }
                }
                break;
            case DELAY:
                performDelayedAttack(AltoClef.INSTANCE);
                break;
            case OFF:
                break;
        }
    }

    private void performDelayedAttack(AltoClef mod) {
        if (!mod.getFoodChain().needsToEat() && !mod.getMLGBucketChain().isFallingOhNo(mod) &&
                mod.getMLGBucketChain().doneMLG() && !mod.getMLGBucketChain().isChorusFruiting()) {
            if (_forceHit != null) {
                attack(_forceHit, true);
            }
            // wait for the attack delay
            if (_targets.isEmpty()) {
                return;
            }

            Optional<Entity> toHit = _targets.stream().min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(mod.getPlayer())));

            if (mod.getPlayer() == null || mod.getPlayer().getAttackCooldownProgress(0) < 1) {
                return;
            }

            toHit.ifPresent(entity -> attack(entity, true));
        }
    }

    private void performFastestAttack(AltoClef mod) {
        if (!mod.getFoodChain().needsToEat() && !mod.getMLGBucketChain().isFallingOhNo(mod) &&
                mod.getMLGBucketChain().doneMLG() && !mod.getMLGBucketChain().isChorusFruiting()) {
            // Just attack whenever you can
            for (Entity entity : _targets) {
                attack(mod, entity);
            }
        }
    }

    private void attack(AltoClef mod, Entity entity) {
        attack(entity, false);
    }

    private void attack(Entity entity, boolean equipSword) {
        if (entity == null) return;
        if (!(entity instanceof FireballEntity)) {
            LookHelper.lookAt(AltoClef.INSTANCE, entity.getEyePos());
        }

        ClientPlayerEntity player = AltoClef.INSTANCE.getPlayer();

        if (Double.isInfinite(_forceFieldRange) || entity.squaredDistanceTo(player) < _forceFieldRange * _forceFieldRange ||
                entity.squaredDistanceTo(player) < 40) {
            if (entity instanceof FireballEntity) {
                AltoClef.INSTANCE.getControllerExtras().attack(entity);
            }
            boolean canAttack;
            if (equipSword) {
                equipWeapon();
                canAttack = true;
            } else {
                // Equip non-tool
                canAttack = AltoClef.INSTANCE.getSlotHandler().forceDeequipHitTool();
            }
            if (canAttack) {
                if (player.isOnGround() || player.getVelocity().getY() < 0 || player.isTouchingWater()) {
                    AltoClef.INSTANCE.getControllerExtras().attack(entity);
                }
            }
        }
    }

    public void startShielding() {
        _shielding = true;
        InputControls inputControls = AltoClef.INSTANCE.getInputControls();
        SlotHandler slotHandler = AltoClef.INSTANCE.getSlotHandler();
        Baritone clientBaritone = AltoClef.INSTANCE.getClientBaritone();
        AltoClefSettings extraBaritoneSettings = AltoClef.INSTANCE.getExtraBaritoneSettings();

        inputControls.hold(Input.SNEAK);
        inputControls.hold(Input.CLICK_RIGHT);

        clientBaritone.getPathingBehavior().softCancelIfSafe();
        extraBaritoneSettings.setInteractionPaused(true);

        if (!AltoClef.INSTANCE.getPlayer().isBlocking()) {
            ItemStack handItem = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot());
            if (handItem.isFood()) {
                List<ItemStack> spaceSlots = AltoClef.INSTANCE.getItemStorage().getItemStacksPlayerInventory(false);
                if (!spaceSlots.isEmpty()) {
                    for (ItemStack spaceSlot : spaceSlots) {
                        if (spaceSlot.isEmpty()) {
                            slotHandler.clickSlot(PlayerSlot.getEquipSlot(), 0, SlotActionType.QUICK_MOVE);
                            return;
                        }
                    }
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(AltoClef.INSTANCE);
                garbage.ifPresent(slot -> slotHandler.forceEquipItem(StorageHelper.getItemStackInSlot(slot).getItem()));
            }
        }
    }

    public void stopShielding() {
        ItemStorageTracker itemStorage = AltoClef.INSTANCE.getItemStorage();
        SlotHandler slotHandler = AltoClef.INSTANCE.getSlotHandler();
        InputControls inputControls = AltoClef.INSTANCE.getInputControls();

        if (_shielding) {
            ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
            if (cursor.isFood()) {
                Optional<Slot> toMoveTo = itemStorage.getSlotThatCanFitInPlayerInventory(cursor, false).or(() -> StorageHelper.getGarbageSlot(AltoClef.INSTANCE));
                if (toMoveTo.isPresent()) {
                    Slot garbageSlot = toMoveTo.get();
                    slotHandler.clickSlot(garbageSlot, 0, SlotActionType.PICKUP);
                }
            }
            inputControls.release(Input.SNEAK);
            inputControls.release(Input.CLICK_RIGHT);
            inputControls.release(Input.JUMP);
            AltoClef.INSTANCE.getExtraBaritoneSettings().setInteractionPaused(false);
            _shielding = false;
        }
    }

    public enum Strategy {
        OFF,
        FASTEST,
        DELAY,
        SMART
    }
}
