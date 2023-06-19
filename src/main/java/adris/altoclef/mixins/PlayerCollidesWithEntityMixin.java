package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.PlayerCollidedWithEntityEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(Entity.class)
public class PlayerCollidesWithEntityMixin {

    // Determines a collision between items/EXP orbs/other objects within "pickup" range.
    @Inject(
            method = "onPlayerCollision",
            at = @At("HEAD")
    )
    private void onCollideWithEntity(PlayerEntity player) {
        // TODO: Less hard-coded manual means of enforcing client side access
        if (player instanceof ClientPlayerEntity) {
            EventBus.publish(new PlayerCollidedWithEntityEvent(player, (Entity) (Object) this));
        }
    }
}
