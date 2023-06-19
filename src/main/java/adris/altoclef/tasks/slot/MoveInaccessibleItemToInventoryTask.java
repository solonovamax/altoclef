package adris.altoclef.tasks.slot;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CursorSlot;
import adris.altoclef.util.slots.Slot;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Objects;
import java.util.Optional;

public class MoveInaccessibleItemToInventoryTask extends Task {

    private final ItemTarget _target;

    public MoveInaccessibleItemToInventoryTask(ItemTarget target) {
        _target = target;
    }

    @Override
    protected void onStart() {

    }

    @Override
    protected Task onTick() {
        // Ensure inventory is closed.
        if (!StorageHelper.isPlayerInventoryOpen()) {
            ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
            if (!cursorStack.isEmpty()) {
                Optional<Slot> moveTo = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
                if (moveTo.isPresent()) {
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                    return null;
                }
                if (ItemHelper.canThrowAwayStack(AltoClef.INSTANCE, cursorStack)) {
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    return null;
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(AltoClef.INSTANCE);
                // Try throwing away cursor slot if it's garbage
                if (garbage.isPresent()) {
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                    return null;
                }
                AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            } else {
                StorageHelper.closeScreen();
            }
            setDebugState("Closing screen first (hope this doesn't get spammed a million times)");
            return null;
        }

        Optional<Slot> slotToMove = StorageHelper.getFilledInventorySlotInaccessibleToContainer(AltoClef.INSTANCE, _target);
        if (slotToMove.isPresent()) {
            // Force cursor slot if we have one.
            if (_target.matches(StorageHelper.getItemStackInCursorSlot().getItem())) {
                slotToMove = Optional.of(CursorSlot.SLOT);
            }
            // issue is a full cursor slot when trying to clear out bad items.
            // solution: ensure cursor is empty first
            if (!StorageHelper.getItemStackInCursorSlot().isEmpty()) {
                return new EnsureFreeCursorSlotTask();
            }

            Slot toMove = slotToMove.get();
            ItemStack stack = StorageHelper.getItemStackInSlot(toMove);
            Optional<Slot> toMoveTo = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(stack, false);
            if (toMoveTo.isPresent()) {
                setDebugState("Moving slot " + toMove + " to inventory");
                // Pick up & move
                if (Slot.isCursor(toMove)) {
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(toMoveTo.get(), 0, SlotActionType.PICKUP);
                } else {
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(toMove, 0, SlotActionType.PICKUP);
                }
                return null;
            } else {
                setDebugState("Free up inventory first.");
                // Make it free first.
                return new EnsureFreeInventorySlotTask();
            }
        }
        setDebugState("NONE FOUND");
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof MoveInaccessibleItemToInventoryTask task) {
            return Objects.equals(task._target, _target);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Making item accessible: " + _target;
    }
}
