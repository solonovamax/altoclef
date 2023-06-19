package adris.altoclef.tasksystem;

import gay.solonovamax.altoclef.AltoClef;

/**
 * Some tasks may mess up royally if we interrupt them while mid air.
 * For instance, if we're doing some parkour and a baritone task is stopped,
 * the player will fall to whatever is below them, perhaps their death.
 */
public interface ITaskRequiresGrounded extends ITaskCanForce {
    @Override
    default boolean shouldForce(Task interruptingCandidate) {
        if (interruptingCandidate instanceof ITaskOverridesGrounded)
            return false;
        return !(AltoClef.INSTANCE.getPlayer().isOnGround() || AltoClef.INSTANCE.getPlayer().isSwimming() || AltoClef.INSTANCE.getPlayer().isTouchingWater() || AltoClef.INSTANCE.getPlayer().isClimbing());
    }
}
