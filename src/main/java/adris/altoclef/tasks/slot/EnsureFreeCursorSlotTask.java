package adris.altoclef.tasks.slot;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;

public class EnsureFreeCursorSlotTask extends Task {

    @Override
    protected void onStart() {
        // YEET
    }

    @Override
    protected Task onTick() {
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();

        if (!cursor.isEmpty()) {
            Optional<Slot> moveTo = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
            if (moveTo.isPresent()) {
                setDebugState("Moving cursor stack back");
                AltoClef.INSTANCE.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            if (ItemHelper.canThrowAwayStack(AltoClef.INSTANCE, cursor)) {
                setDebugState("Incompatible cursor stack, throwing");
                AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            } else {
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(AltoClef.INSTANCE);
                if (garbage.isPresent()) {
                    // Pick up garbage so we throw it out next frame
                    setDebugState("Picking up garbage");
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                } else {
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                }
            }
            return null;
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof EnsureFreeCursorSlotTask;
    }


    // And filling this in will make it look ok in the task tree
    @Override
    protected String toDebugString() {
        return "Breaking the cursor slot";
    }
}
