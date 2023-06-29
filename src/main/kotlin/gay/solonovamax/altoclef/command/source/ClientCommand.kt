package gay.solonovamax.altoclef.command.source

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.audience.ForwardingAudience
import net.kyori.adventure.platform.fabric.FabricClientAudiences

data class ClientCommand(val source: FabricClientCommandSource) : ForwardingAudience.Single {
    private val audience: Audience = FabricClientAudiences.of().audience()
    override fun audience(): Audience = audience
}

