package adris.altoclef.tasks.slot;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;

public class EnsureFreeInventorySlotTask extends Task {
    @Override
    protected void onStart() {

    }

    @Override
    protected Task onTick() {
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        Optional<Slot> garbage = StorageHelper.getGarbageSlot(AltoClef.INSTANCE);
        if (cursorStack.isEmpty()) {
            if (garbage.isPresent()) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                return null;
            }
        }
        if (!cursorStack.isEmpty()) {
            LookHelper.randomOrientation(AltoClef.INSTANCE);
            AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            return null;
        }
        setDebugState("All items are protected.");
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof EnsureFreeInventorySlotTask;
    }

    @Override
    protected String toDebugString() {
        return "Ensuring inventory is free";
    }
}
