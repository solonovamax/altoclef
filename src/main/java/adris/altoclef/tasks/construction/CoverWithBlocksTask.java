package adris.altoclef.tasks.construction;

import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CoverWithBlocksTask extends Task {
    private static final TimerGame timer = new TimerGame(30);
    private static final Task goToNether = new DefaultGoToDimensionTask(Dimension.NETHER);
    private static final Task goToOverworld = new DefaultGoToDimensionTask(Dimension.OVERWORLD);
    private static Task getBlocks;
    private BlockPos lavaPos;

    @Override
    protected void onStart() {
        timer.reset();
        AltoClef.INSTANCE.getBlockTracker().trackBlock(Blocks.LAVA);
    }

    @Override
    protected Task onTick() {
        Item[] throwAways = AltoClef.INSTANCE.getModSettings().getThrowawayItems(true);
        Item[] throwAwaysToUse = Arrays.stream(throwAways).filter(item -> !(Block.getBlockFromItem(item) instanceof LeavesBlock) &&
                !(Block.getBlockFromItem(item) instanceof FallingBlock) && item instanceof BlockItem).toArray(Item[]::new);
        int throwAwayCount = AltoClef.INSTANCE.getItemStorage().getItemCount(throwAwaysToUse);
        if (getBlocks != null && throwAwayCount < 128) {
            setDebugState("Getting blocks to cover nether lava.");
            timer.reset();
            return getBlocks;
        } else {
            getBlocks = null;
        }
        Block[] blocks = ItemHelper.itemsToBlocks(throwAwaysToUse);
        if (!AltoClef.INSTANCE.getItemStorage().hasItem(throwAwaysToUse) && AltoClef.INSTANCE.getBlockTracker().anyFound(blocks)) {
            timer.reset();
            ItemTarget throwAwaysTarget = new ItemTarget(throwAwaysToUse);
            getBlocks = new MineAndCollectTask(throwAwaysTarget, blocks, MiningRequirement.STONE);
            return getBlocks;
        }
        if (!AltoClef.INSTANCE.getItemStorage().hasItem(throwAwaysToUse) && !AltoClef.INSTANCE.getBlockTracker().anyFound(blocks)) {
            timer.reset();
            if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD) {
                setDebugState("Trying nether to search for blocks.");
                return goToNether;
            }
            if (WorldHelper.getCurrentDimension() == Dimension.NETHER) {
                setDebugState("Trying overworld to search for blocks.");
                return goToOverworld;
            }
        }
        if (WorldHelper.getCurrentDimension() != Dimension.NETHER) {
            setDebugState("Going to nether.");
            timer.reset();
            return goToNether;
        }
        if (coverLavaWithSand(AltoClef.INSTANCE) == null) {
            setDebugState("Searching valid lava.");
            timer.reset();
            return new TimeoutWanderTask();
        }
        setDebugState("Covering lava with blocks");
        return coverLavaWithSand(AltoClef.INSTANCE);
    }

    private Task coverLavaWithSand(AltoClef mod) {
        Predicate<BlockPos> validLava = blockPos ->
                mod.getWorld().getBlockState(blockPos).getFluidState().isStill() &&
                        WorldHelper.isAir(mod, blockPos.up()) &&
                        (!WorldHelper.isBlock(blockPos.north(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.south(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.east(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.west(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.north().up(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.south().up(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.east().up(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(blockPos.west().up(), Blocks.LAVA));
        Optional<BlockPos> lava = mod.getBlockTracker().getNearestTracking(validLava, Blocks.LAVA);
        if (lava.isPresent()) {
            if (lavaPos == null) {
                lavaPos = lava.get();
                timer.reset();
            }
            if (timer.elapsed()) {
                lavaPos = lava.get();
                timer.reset();
            }
            if (!WorldHelper.isBlock(lavaPos, Blocks.LAVA) || !WorldHelper.isAir(mod, lavaPos.up()) ||
                    !mod.getWorld().getBlockState(lavaPos).getFluidState().isStill()) {
                lavaPos = lava.get();
                timer.reset();
            }
            Item[] throwAways = mod.getModSettings().getThrowawayItems(true);
            Item[] throwAwaysToUse = Arrays.stream(throwAways).filter(item -> !(Block.getBlockFromItem(item) instanceof LeavesBlock) &&
                    !(Block.getBlockFromItem(item) instanceof FallingBlock) && item instanceof BlockItem).toArray(Item[]::new);
            List<Slot> presentThrowAways = mod.getItemStorage().getSlotsWithItemPlayerInventory(true, throwAwaysToUse);
            if (!presentThrowAways.isEmpty()) {
                for (Slot presentThrowAway : presentThrowAways) {
                    Item throwAwayItem = StorageHelper.getItemStackInSlot(presentThrowAway).getItem();
                    Block throwAwayBlock = Block.getBlockFromItem(throwAwayItem);
                    return new PlaceBlockTask(lavaPos, throwAwayBlock);
                }
            }
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getBlockTracker().stopTracking(Blocks.LAVA);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof CoverWithBlocksTask;
    }

    @Override
    protected String toDebugString() {
        return "Covering nether lava with blocks";
    }
}
