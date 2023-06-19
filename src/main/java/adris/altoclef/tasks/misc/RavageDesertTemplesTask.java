package adris.altoclef.tasks.misc;

import adris.altoclef.tasks.movement.SearchWithinBiomeTask;
import adris.altoclef.tasks.squashed.CataloguedResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;

import java.util.List;

public class RavageDesertTemplesTask extends Task {
    public final Item[] LOOT = {
            Items.BONE,
            Items.ROTTEN_FLESH,
            Items.GUNPOWDER,
            Items.SAND,
            Items.STRING,
            Items.SPIDER_EYE,
            Items.ENCHANTED_BOOK,
            Items.SADDLE,
            Items.GOLDEN_APPLE,
            Items.GOLD_INGOT,
            Items.IRON_INGOT,
            Items.EMERALD,
            Items.IRON_HORSE_ARMOR,
            Items.GOLDEN_HORSE_ARMOR,
            Items.DIAMOND,
            Items.DIAMOND_HORSE_ARMOR,
            Items.ENCHANTED_GOLDEN_APPLE
    };
    private BlockPos _currentTemple;
    private Task _lootTask;
    private Task _pickaxeTask;

    public RavageDesertTemplesTask() {

    }

    @Override
    protected void onStart() {
        AltoClef.INSTANCE.getBehaviour().push();
        AltoClef.INSTANCE.getBlockTracker().trackBlock(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected Task onTick() {
        if (_pickaxeTask != null && !_pickaxeTask.isFinished()) {
            setDebugState("Need to get pickaxes first");
            return _pickaxeTask;
        }
        if (_lootTask != null && !_lootTask.isFinished()) {
            setDebugState("Looting found temple");
            return _lootTask;
        }
        if (StorageHelper.miningRequirementMetInventory(AltoClef.INSTANCE, MiningRequirement.WOOD)) {
            setDebugState("Need to get pickaxes first");
            _pickaxeTask = new CataloguedResourceTask(new ItemTarget(Items.WOODEN_PICKAXE, 2));
            return _pickaxeTask;
        }
        _currentTemple = WorldHelper.getADesertTemple(AltoClef.INSTANCE);
        if (_currentTemple != null) {
            _lootTask = new LootDesertTempleTask(_currentTemple, List.of(LOOT));
            setDebugState("Looting found temple");
            return _lootTask;
        }
        return new SearchWithinBiomeTask(BiomeKeys.DESERT);
    }

    @Override
    protected void onStop(Task task) {
        AltoClef.INSTANCE.getBlockTracker().stopTracking(Blocks.STONE_PRESSURE_PLATE);
        AltoClef.INSTANCE.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof RavageDesertTemplesTask;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Ravaging Desert Temples";
    }
}
