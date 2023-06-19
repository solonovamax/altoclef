package adris.altoclef.tasks.slot;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.Slot;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.screen.slot.SlotActionType;

public class ClickSlotTask extends Task {

    private final Slot _slot;
    private final int _mouseButton;
    private final SlotActionType _type;

    private boolean _clicked = false;

    public ClickSlotTask(Slot slot, int mouseButton, SlotActionType type) {
        _slot = slot;
        _mouseButton = mouseButton;
        _type = type;
    }

    public ClickSlotTask(Slot slot, SlotActionType type) {
        this(slot, 0, type);
    }

    public ClickSlotTask(Slot slot, int mouseButton) {
        this(slot, mouseButton, SlotActionType.PICKUP);
    }

    public ClickSlotTask(Slot slot) {
        this(slot, SlotActionType.PICKUP);
    }

    @Override
    protected void onStart() {
        _clicked = false;
    }

    @Override
    protected Task onTick() {
        if (AltoClef.INSTANCE.getSlotHandler().canDoSlotAction()) {
            AltoClef.INSTANCE.getSlotHandler().clickSlot(_slot, _mouseButton, _type);
            AltoClef.INSTANCE.getSlotHandler().registerSlotAction();
            _clicked = true;
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ClickSlotTask task) {
            return task._mouseButton == _mouseButton && task._type == _type && task._slot.equals(_slot);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Clicking " + _slot.toString();
    }

    @Override
    public boolean isFinished() {
        return _clicked;
    }
}
