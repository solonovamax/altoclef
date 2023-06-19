package adris.altoclef.tasks.construction.compound;

import adris.altoclef.BotBehaviour;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.squashed.CataloguedResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.EntityTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.Settings;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class ConstructIronGolemTask extends Task {
    private BlockPos _position;
    private boolean _canBeFinished = false;

    public ConstructIronGolemTask() {

    }

    public ConstructIronGolemTask(BlockPos pos) {
        _position = pos;
    }

    @Override
    protected void onStart() {
        BotBehaviour behaviour = AltoClef.INSTANCE.getBehaviour();
        Settings clientBaritoneSettings = AltoClef.INSTANCE.getClientBaritoneSettings();

        behaviour.push();
        behaviour.addProtectedItems(Items.IRON_BLOCK, Items.CARVED_PUMPKIN);
        clientBaritoneSettings.blocksToAvoidBreaking.value.add(Blocks.IRON_BLOCK);
    }

    @Override
    protected Task onTick() {
        if (!StorageHelper.itemTargetsMetInventory(golemMaterials())) {
            setDebugState("Getting materials for the iron golem");
            return new CataloguedResourceTask(golemMaterials());
        }
        if (_position == null) {
            ClientPlayerEntity player = AltoClef.INSTANCE.getPlayer();
            ClientWorld world = AltoClef.INSTANCE.getWorld();

            for (BlockPos pos : WorldHelper.scanRegion(
                    new BlockPos(player.getBlockX(), 64, player.getBlockZ()),
                    new BlockPos(player.getBlockX(), 128, player.getBlockZ())
            )) {
                if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                    _position = pos;
                    break;
                }
            }
            if (_position == null) {
                _position = player.getBlockPos();
            }
        }
        if (!WorldHelper.isBlock(_position, Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(_position, Blocks.AIR)) {
                setDebugState("Destroying block in way of base iron block");
                return new DestroyBlockTask(_position);
            }
            setDebugState("Placing the base iron block");
            return new PlaceBlockTask(_position, Blocks.IRON_BLOCK);
        }
        //        mod.getPlayer().getServer().getPlayerManager().getPlayer("camelCasedSnivy").getAdvancementTracker()
        if (!WorldHelper.isBlock(_position.up(), Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(_position.up(), Blocks.AIR)) {
                setDebugState("Destroying block in way of center iron block");
                return new DestroyBlockTask(_position.up());
            }
            setDebugState("Placing the center iron block");
            return new PlaceBlockTask(_position.up(), Blocks.IRON_BLOCK);
        }
        if (!WorldHelper.isBlock(_position.up().east(), Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(_position.up().east(), Blocks.AIR)) {
                setDebugState("Destroying block in way of east iron block");
                return new DestroyBlockTask(_position.up().east());
            }
            setDebugState("Placing the east iron block");
            return new PlaceBlockTask(_position.up().east(), Blocks.IRON_BLOCK);
        }
        if (!WorldHelper.isBlock(_position.up().west(), Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(_position.up().west(), Blocks.AIR)) {
                setDebugState("Destroying block in way of west iron block");
                return new DestroyBlockTask(_position.up().west());
            }
            setDebugState("Placing the west iron block");
            return new PlaceBlockTask(_position.up().west(), Blocks.IRON_BLOCK);
        }
        if (!WorldHelper.isBlock(_position.east(), Blocks.AIR)) {
            setDebugState("Clearing area on east side...");
            return new DestroyBlockTask(_position.east());
        }
        if (!WorldHelper.isBlock(_position.west(), Blocks.AIR)) {
            setDebugState("Clearing area on west side...");
            return new DestroyBlockTask(_position.west());
        }
        if (!WorldHelper.isBlock(_position.up(2), Blocks.AIR)) {
            setDebugState("Destroying block in way of pumpkin");
            return new DestroyBlockTask(_position.up(2));
        }
        _canBeFinished = true;
        setDebugState("Placing the pumpkin (I think)");
        return new PlaceBlockTask(_position.up(2), Blocks.CARVED_PUMPKIN);
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.INSTANCE.getClientBaritoneSettings().blocksToAvoidBreaking.value.remove(Blocks.IRON_BLOCK);
        AltoClef.INSTANCE.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof ConstructIronGolemTask;
    }

    @Override
    public boolean isFinished() {
        if (_position == null) return false;
        EntityTracker entityTracker = AltoClef.INSTANCE.getEntityTracker();
        Optional<Entity> closestIronGolem = entityTracker.getClosestEntity(new Vec3d(_position.getX(), _position.getY(), _position.getZ()), IronGolemEntity.class);
        return closestIronGolem.isPresent() && closestIronGolem.get().getBlockPos().isWithinDistance(_position, 2) && _canBeFinished;
    }

    @Override
    protected String toDebugString() {
        return "Construct Iron Golem";
    }

    private int ironBlocksNeeded() {
        if (_position == null) {
            return 4;
        }
        int needed = 0;
        ClientWorld world = AltoClef.INSTANCE.getWorld();

        if (world.getBlockState(_position).getBlock() != Blocks.IRON_BLOCK)
            needed++;
        if (world.getBlockState(_position.up().west()).getBlock() != Blocks.IRON_BLOCK)
            needed++;
        if (world.getBlockState(_position.up().east()).getBlock() != Blocks.IRON_BLOCK)
            needed++;
        if (world.getBlockState(_position.up()).getBlock() != Blocks.IRON_BLOCK)
            needed++;
        return needed;
    }

    private ItemTarget[] golemMaterials() {
        if (_position == null || AltoClef.INSTANCE.getWorld().getBlockState(_position.up(2)).getBlock() != Blocks.CARVED_PUMPKIN)
            return new ItemTarget[]{
                    new ItemTarget(Items.IRON_BLOCK, ironBlocksNeeded()),
                    new ItemTarget(Items.CARVED_PUMPKIN, 1)
            };
        else return new ItemTarget[]{
                new ItemTarget(Items.IRON_BLOCK, ironBlocksNeeded())
        };
    }
}
