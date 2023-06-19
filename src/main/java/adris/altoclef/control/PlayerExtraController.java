package adris.altoclef.control;

import adris.altoclef.Settings;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockBreakingCancelEvent;
import adris.altoclef.eventbus.events.BlockBreakingEvent;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class PlayerExtraController {
    private BlockPos _blockBreakPos;
    private double _blockBreakProgress;

    public PlayerExtraController() {
        EventBus.subscribe(BlockBreakingEvent.class, evt -> onBlockBreak(evt.blockPos, evt.progress));
        EventBus.subscribe(BlockBreakingCancelEvent.class, evt -> onBlockStopBreaking());
    }

    private void onBlockBreak(BlockPos pos, double progress) {
        _blockBreakPos = pos;
        _blockBreakProgress = progress;
    }

    private void onBlockStopBreaking() {
        _blockBreakPos = null;
        _blockBreakProgress = 0;
    }

    public BlockPos getBreakingBlockPos() {
        return _blockBreakPos;
    }

    public boolean isBreakingBlock() {
        return _blockBreakPos != null;
    }

    public double getBreakingBlockProgress() {
        return _blockBreakProgress;
    }

    public boolean inRange(Entity entity) {
        ClientPlayerEntity player = AltoClef.INSTANCE.getPlayer();
        Settings modSettings = AltoClef.INSTANCE.getModSettings();

        return player.isInRange(entity, modSettings.getEntityReachRange());
    }

    public void attack(Entity entity) {
        if (inRange(entity)) {
            ClientPlayerInteractionManager controller = AltoClef.INSTANCE.getController();
            ClientPlayerEntity player = AltoClef.INSTANCE.getPlayer();

            controller.attackEntity(player, entity);
            player.swingHand(Hand.MAIN_HAND);
        }
    }
}
