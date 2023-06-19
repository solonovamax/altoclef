package adris.altoclef.chains;

import adris.altoclef.Debug;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;

public class PlayerInteractionFixChain extends TaskChain {
    private final TimerGame _stackHeldTimeout = new TimerGame(1);
    private final TimerGame _generalDuctTapeSwapTimeout = new TimerGame(30);
    private final TimerGame _shiftDepressTimeout = new TimerGame(10);
    private final TimerGame _betterToolTimer = new TimerGame(0);
    private final TimerGame _mouseMovingButScreenOpenTimeout = new TimerGame(1);
    private ItemStack _lastHandStack = null;

    private Screen _lastScreen;
    private Rotation _lastLookRotation;

    public PlayerInteractionFixChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onStop() {

    }

    @Override
    public void onInterrupt(TaskChain other) {

    }

    @Override
    protected void onTick() {
    }

    @Override
    public float getPriority() {

        if (!AltoClef.inGame()) return Float.NEGATIVE_INFINITY;

        if (AltoClef.INSTANCE.getUserTaskChain().isActive() && _betterToolTimer.elapsed()) {
            // Equip the right tool for the job if we're not using one.
            _betterToolTimer.reset();
            if (AltoClef.INSTANCE.getControllerExtras().isBreakingBlock()) {
                BlockState state = AltoClef.INSTANCE.getWorld().getBlockState(AltoClef.INSTANCE.getControllerExtras().getBreakingBlockPos());
                Optional<Slot> bestToolSlot = StorageHelper.getBestToolSlot(AltoClef.INSTANCE, state);
                Slot currentEquipped = PlayerSlot.getEquipSlot();

                // if baritone is running, only accept tools OUTSIDE OF HOTBAR!
                // Baritone will take care of tools inside the hotbar.
                if (bestToolSlot.isPresent() && !bestToolSlot.get().equals(currentEquipped)) {
                    // ONLY equip if the item class is STRICTLY different (otherwise we swap around a lot)
                    if (StorageHelper.getItemStackInSlot(currentEquipped).getItem() != StorageHelper.getItemStackInSlot(bestToolSlot.get()).getItem()) {
                        boolean isAllowedToManage = (!AltoClef.INSTANCE.getClientBaritone().getPathingBehavior().isPathing() ||
                                bestToolSlot.get().getInventorySlot() >= 9) && !AltoClef.INSTANCE.getFoodChain().isTryingToEat();
                        if (isAllowedToManage) {
                            Debug.logMessage("Found better tool in inventory, equipping.");
                            ItemStack bestToolItemStack = StorageHelper.getItemStackInSlot(bestToolSlot.get());
                            Item bestToolItem = bestToolItemStack.getItem();
                            AltoClef.INSTANCE.getSlotHandler().forceEquipItem(bestToolItem);
                        }
                    }
                }
            }
        }

        // Unpress shift (it gets stuck for some reason???)
        if (AltoClef.INSTANCE.getInputControls().isHeldDown(Input.SNEAK)) {
            if (_shiftDepressTimeout.elapsed()) {
                AltoClef.INSTANCE.getInputControls().release(Input.SNEAK);
            }
        } else {
            _shiftDepressTimeout.reset();
        }

        // Refresh inventory
        if (_generalDuctTapeSwapTimeout.elapsed()) {
            if (!AltoClef.INSTANCE.getControllerExtras().isBreakingBlock()) {
                Debug.logMessage("Refreshed inventory...");
                AltoClef.INSTANCE.getSlotHandler().refreshInventory();
                _generalDuctTapeSwapTimeout.reset();
                return Float.NEGATIVE_INFINITY;
            }
        }

        ItemStack currentStack = StorageHelper.getItemStackInCursorSlot();

        if (currentStack != null && !currentStack.isEmpty()) {
            //noinspection PointlessNullCheck
            if (_lastHandStack == null || !ItemStack.areEqual(currentStack, _lastHandStack)) {
                // We're holding a new item in our stack!
                _stackHeldTimeout.reset();
                _lastHandStack = currentStack.copy();
            }
        } else {
            _stackHeldTimeout.reset();
            _lastHandStack = null;
        }

        // If we have something in our hand for a period of time...
        if (_lastHandStack != null && _stackHeldTimeout.elapsed()) {
            Optional<Slot> moveTo = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(_lastHandStack, false);
            if (moveTo.isPresent()) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                return Float.NEGATIVE_INFINITY;
            }
            if (ItemHelper.canThrowAwayStack(AltoClef.INSTANCE, StorageHelper.getItemStackInCursorSlot())) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return Float.NEGATIVE_INFINITY;
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(AltoClef.INSTANCE);
            // Try throwing away cursor slot if it's garbage
            if (garbage.isPresent()) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                return Float.NEGATIVE_INFINITY;
            }
            AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            return Float.NEGATIVE_INFINITY;
        }

        if (shouldCloseOpenScreen(AltoClef.INSTANCE)) {
            // Debug.logMessage("Closed screen since we changed our look.");
            ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
            if (!cursorStack.isEmpty()) {
                Optional<Slot> moveTo = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
                if (moveTo.isPresent()) {
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                    return Float.NEGATIVE_INFINITY;
                }
                if (ItemHelper.canThrowAwayStack(AltoClef.INSTANCE, cursorStack)) {
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    return Float.NEGATIVE_INFINITY;
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(AltoClef.INSTANCE);
                // Try throwing away cursor slot if it's garbage
                if (garbage.isPresent()) {
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                    return Float.NEGATIVE_INFINITY;
                }
                AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            } else {
                StorageHelper.closeScreen();
            }
            return Float.NEGATIVE_INFINITY;
        }

        return Float.NEGATIVE_INFINITY;
    }

    private boolean shouldCloseOpenScreen(AltoClef mod) {
        if (!mod.getModSettings().shouldCloseScreenWhenLookingOrMining())
            return false;
        // Only check look if we've had the same screen open for a while
        Screen openScreen = MinecraftClient.getInstance().currentScreen;
        if (openScreen != _lastScreen) {
            _mouseMovingButScreenOpenTimeout.reset();
        }
        // We're in the player screen/a screen we DON'T want to cancel out of
        if (openScreen == null || openScreen instanceof ChatScreen || openScreen instanceof GameMenuScreen || openScreen instanceof DeathScreen) {
            _mouseMovingButScreenOpenTimeout.reset();
            return false;
        }
        // Check for rotation change
        Rotation look = LookHelper.getLookRotation();
        if (_lastLookRotation != null && _mouseMovingButScreenOpenTimeout.elapsed()) {
            Rotation delta = look.subtract(_lastLookRotation);
            if (Math.abs(delta.getYaw()) > 0.1f || Math.abs(delta.getPitch()) > 0.1f) {
                _lastLookRotation = look;
                return true;
            }
            // do NOT update our last look rotation, just because we want to measure long term rotation.
        } else {
            _lastLookRotation = look;
        }
        _lastScreen = openScreen;
        return false;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getName() {
        return "Hand Stack Fix Chain";
    }
}
