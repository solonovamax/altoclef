package adris.altoclef.tasks;

import adris.altoclef.tasks.slot.MoveItemToSlotFromInventoryTask;
import adris.altoclef.tasks.slot.ReceiveCraftingOutputSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;

/**
 * Assuming a crafting screen is open, crafts a recipe.
 * <p>
 * Not useful for custom tasks.
 */
public class CraftGenericManuallyTask extends Task {
    private final RecipeTarget _target;

    public CraftGenericManuallyTask(RecipeTarget target) {
        _target = target;
    }

    @Override
    protected void onStart() {

    }

    @Override
    protected Task onTick() {

        boolean bigCrafting = StorageHelper.isBigCraftingOpen();

        if (!bigCrafting && !StorageHelper.isPlayerInventoryOpen()) {
            // Make sure we're not in another screen before we craft,
            // otherwise crafting won't work
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
            // Just to be safe
        }

        Slot outputSlot = bigCrafting ? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;

        // Example:
        // We need 9 sticks
        // plank recipe results in 4 sticks
        // this means 3 planks per slot
        int requiredPerSlot = (int) Math.ceil((double) _target.getTargetCount() / _target.getRecipe().outputCount());

        // For each slot in table
        for (int craftSlot = 0; craftSlot < _target.getRecipe().getSlotCount(); ++craftSlot) {
            ItemTarget toFill = _target.getRecipe().getSlot(craftSlot);
            Slot currentCraftSlot;
            if (bigCrafting) {
                // Craft in table
                currentCraftSlot = CraftingTableSlot.getInputSlot(craftSlot, _target.getRecipe().isBig());
            } else {
                // Craft in window
                currentCraftSlot = PlayerSlot.getCraftInputSlot(craftSlot);
            }
            ItemStack present = StorageHelper.getItemStackInSlot(currentCraftSlot);
            if (toFill == null || toFill.isEmpty()) {
                if (present.getItem() != Items.AIR) {
                    // Move this item OUT if it should be empty
                    setDebugState("Found INVALID slot");
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(currentCraftSlot, 0, SlotActionType.PICKUP);
                }
            } else {
                boolean correctItem = toFill.matches(present.getItem());
                boolean isSatisfied = correctItem && present.getCount() >= requiredPerSlot;
                if (!isSatisfied) {
                    // We have items that satisfy, but we CAN NOT fill in the current slot!
                    // In that case, just grab from the output.
                    if (!AltoClef.INSTANCE.getItemStorage().hasItemInventoryOnly(present.getItem())) {
                        if (!StorageHelper.getItemStackInSlot(outputSlot).isEmpty()) {
                            setDebugState("NO MORE to fit: grabbing from output.");
                            return new ReceiveCraftingOutputSlotTask(outputSlot, _target.getTargetCount());
                        } else {
                            // Move on to the NEXT slot, we can't fill this one anymore.
                            continue;
                        }
                    }

                    setDebugState("Moving item to slot...");
                    return new MoveItemToSlotFromInventoryTask(new ItemTarget(toFill, requiredPerSlot), currentCraftSlot);
                }
                // We could be OVER satisfied
                boolean oversatisfies = present.getCount() > requiredPerSlot;
                if (oversatisfies) {
                    setDebugState("OVER SATISFIED slot! Right clicking slot to extract half and spread it out more.");
                    AltoClef.INSTANCE.getSlotHandler().clickSlot(currentCraftSlot, 0, SlotActionType.PICKUP);
                }
            }
        }

        // Ensure our cursor is empty/can receive our item
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (!ItemHelper.canStackTogether(StorageHelper.getItemStackInSlot(outputSlot), cursor)) {
            Optional<Slot> toFit = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false).or(() -> StorageHelper.getGarbageSlot(AltoClef.INSTANCE));
            if (toFit.isPresent()) {
                AltoClef.INSTANCE.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
            } else {
                // Eh screw it
                AltoClef.INSTANCE.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }
        }

        if (!StorageHelper.getItemStackInSlot(outputSlot).isEmpty()) {
            return new ReceiveCraftingOutputSlotTask(outputSlot, _target.getTargetCount());
        } else {
            // Wait
            return null;
        }
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CraftGenericManuallyTask task) {
            return task._target.equals(_target);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Crafting: " + _target;
    }
}
