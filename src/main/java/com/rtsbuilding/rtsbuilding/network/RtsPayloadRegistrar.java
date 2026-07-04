package com.rtsbuilding.rtsbuilding.network;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import com.rtsbuilding.rtsbuilding.network.blueprint.BlueprintPayloadRegistrar;
import com.rtsbuilding.rtsbuilding.network.builder.RtsBuilderPackets;
import com.rtsbuilding.rtsbuilding.network.camera.RtsCameraPackets;
import com.rtsbuilding.rtsbuilding.network.craft.RtsCraftPackets;
import com.rtsbuilding.rtsbuilding.network.feedback.RtsFeedbackPackets;
import com.rtsbuilding.rtsbuilding.network.pathfinding.RtsPathfindingPackets;
import com.rtsbuilding.rtsbuilding.network.plugin.RtsPluginPackets;
import com.rtsbuilding.rtsbuilding.network.progression.RtsProgressionPackets;
import com.rtsbuilding.rtsbuilding.network.storage.RtsStoragePackets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Main registration entry point for non-blueprint RTS packets.
 *
 * The protocol version, payload ids, codecs, and packet directions are still
 * owned by the individual payload records. The domain registrars below are only
 * a readability layer, so moving a payload between them must not change the
 * wire protocol.
 *
 * <p>This mod is server-required, client-optional. All payloads are registered
 * using the optional registrar, allowing clients without the mod installed to
 * connect to the server. The server-side logic will gracefully handle cases where
 * certain packets are not sent by the client.</p>
 */
@EventBusSubscriber(modid = RtsbuildingMod.MODID)
public final class RtsPayloadRegistrar {
    private RtsPayloadRegistrar() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        PayloadRegistrar optionalRegistrar = registrar.optional();

        RtsCameraPackets.register(optionalRegistrar);
        RtsStoragePackets.register(optionalRegistrar);
        RtsBuilderPackets.register(optionalRegistrar);
        RtsCraftPackets.register(optionalRegistrar);
        RtsProgressionPackets.register(optionalRegistrar);
        RtsPluginPackets.register(optionalRegistrar);
        RtsFeedbackPackets.register(optionalRegistrar);
        RtsPathfindingPackets.register(optionalRegistrar);
        BlueprintPayloadRegistrar.register(optionalRegistrar);

        optionalRegistrar.playToServer(C2SRtsModPresentPayload.TYPE, C2SRtsModPresentPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            com.rtsbuilding.rtsbuilding.server.RtsClientModTracker.markClientHasMod(serverPlayer);
                        }
                    });
                });
    }
}
