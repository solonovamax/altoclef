package adris.altoclef.tasks.container;

import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;


public class LootContainerTask extends Task {
    public final BlockPos chest;
    public final List<Item> targets = new ArrayList<>();
    private final Predicate<ItemStack> _check;
    private boolean _weDoneHere = false;

    public LootContainerTask(BlockPos chestPos, List<Item> items) {
        chest = chestPos;
        targets.addAll(items);
        _check = x -> true;
    }

    public LootContainerTask(BlockPos chestPos, List<Item> items, Predicate<ItemStack> pred) {
        chest = chestPos;
        targets.addAll(items);
        _check = pred;
    }

    @Override
    protected void onStart() {
        AltoClef.INSTANCE.getBehaviour().push();
        for (Item item : targets) {
            if (!AltoClef.INSTANCE.getBehaviour().isProtected(item)) {
                AltoClef.INSTANCE.getBehaviour().addProtectedItems(item);
            }
        }
    }

    @Override
    protected Task onTick() {
        if (!ContainerType.screenHandlerMatches(ContainerType.CHEST)) {
            setDebugState("Interact with container");
            return new InteractWithBlockTask(chest);
        }
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (!cursor.isEmpty()) {
            Optional<Slot> toFit = AltoClef.INSTANCE.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
            if (toFit.isPresent()) {
                setDebugState("Putting cursor in inventory");
                AltoClef.INSTANCE.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
                return null;
            } else {
                setDebugState("Ensuring space");
                return new EnsureFreeInventorySlotTask();
            }
        }
        Optional<Slot> optimal = getAMatchingSlot(AltoClef.INSTANCE);
        if (optimal.isEmpty()) {
            _weDoneHere = true;
            return null;
        }
        setDebugState("Looting items: " + targets);
        AltoClef.INSTANCE.getSlotHandler().clickSlot(optimal.get(), 0, SlotActionType.PICKUP);
        return null;
    }

    @Override
    protected void onStop(Task task) {
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
        }
        AltoClef.INSTANCE.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof LootContainerTask && targets == ((LootContainerTask) other).targets;
    }

    private Optional<Slot> getAMatchingSlot(AltoClef mod) {
        for (Item item : targets) {
            List<Slot> slots = mod.getItemStorage().getSlotsWithItemContainer(item);
            if (!slots.isEmpty()) for (Slot slot : slots) {
                if (_check.test(StorageHelper.getItemStackInSlot(slot))) return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isFinished() {
        return _weDoneHere || (ContainerType.screenHandlerMatchesAny() &&
                getAMatchingSlot(AltoClef.INSTANCE).isEmpty());
    }

    @Override
    protected String toDebugString() {
        return "Looting a container";
    }
}
