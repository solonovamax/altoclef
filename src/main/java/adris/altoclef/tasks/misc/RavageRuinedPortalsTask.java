package adris.altoclef.tasks.misc;

import adris.altoclef.tasks.container.LootContainerTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RavageRuinedPortalsTask extends Task {
    public final Item[] LOOT = {
            Items.IRON_NUGGET,
            Items.FLINT,
            Items.OBSIDIAN,
            Items.FIRE_CHARGE,
            Items.FLINT_AND_STEEL,
            Items.GOLD_NUGGET,
            Items.GOLDEN_APPLE,
            Items.GOLDEN_AXE,
            Items.GOLDEN_HOE,
            Items.GOLDEN_PICKAXE,
            Items.GOLDEN_SHOVEL,
            Items.GOLDEN_SWORD,
            Items.GOLDEN_HELMET,
            Items.GOLDEN_CHESTPLATE,
            Items.GOLDEN_LEGGINGS,
            Items.GOLDEN_BOOTS,
            Items.GLISTERING_MELON_SLICE,
            Items.GOLDEN_CARROT,
            Items.GOLD_INGOT,
            Items.CLOCK,
            Items.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Items.GOLDEN_HORSE_ARMOR,
            Items.GOLD_BLOCK,
            Items.BELL,
            Items.ENCHANTED_GOLDEN_APPLE
    };
    private List<BlockPos> _notRuinedPortalChests = new ArrayList<>();
    private Task _lootTask;

    public RavageRuinedPortalsTask() {

    }

    @Override
    protected void onStart() {
        AltoClef.INSTANCE.getBehaviour().push();
        AltoClef.INSTANCE.getBlockTracker().trackBlock(Blocks.CHEST);
    }

    @Override
    protected Task onTick() {
        if (_lootTask != null && _lootTask.isActive() && !_lootTask.isFinished()) {
            return _lootTask;
        }
        Optional<BlockPos> closest = locateClosestUnopenedRuinedPortalChest();
        if (closest.isPresent()) {
            _lootTask = new LootContainerTask(closest.get(), List.of(LOOT));
            return _lootTask;
        }
        return new TimeoutWanderTask();
    }

    @Override
    protected void onStop(Task task) {
        AltoClef.INSTANCE.getBlockTracker().stopTracking(Blocks.CHEST);
        AltoClef.INSTANCE.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task task) {
        return task instanceof RavageRuinedPortalsTask;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Ravaging Ruined Portals";
    }

    private boolean canBeLootablePortalChest(BlockPos blockPos) {
        if (AltoClef.INSTANCE.getWorld().getBlockState(blockPos.up(1)).getBlock() == Blocks.WATER || blockPos.getY() < 50) {
            return false;
        }
        for (BlockPos check : WorldHelper.scanRegion(blockPos.add(-4, -2, -4), blockPos.add(4, 2, 4))) {
            if (AltoClef.INSTANCE.getWorld().getBlockState(check).getBlock() == Blocks.NETHERRACK) {
                return true;
            }
        }
        _notRuinedPortalChests.add(blockPos);
        return false;
    }

    private Optional<BlockPos> locateClosestUnopenedRuinedPortalChest() {
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            return Optional.empty();
        }
        return AltoClef.INSTANCE.getBlockTracker().getNearestTracking(blockPos -> !_notRuinedPortalChests.contains(blockPos) && WorldHelper.isUnopenedChest(AltoClef.INSTANCE, blockPos) && canBeLootablePortalChest(blockPos), Blocks.CHEST);
    }
}
