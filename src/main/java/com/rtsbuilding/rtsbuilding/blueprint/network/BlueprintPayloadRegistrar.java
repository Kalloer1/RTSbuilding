package com.rtsbuilding.rtsbuilding.blueprint.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BlueprintPayloadRegistrar {
    private BlueprintPayloadRegistrar() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
                C2SBlueprintPlacePayload.TYPE,
                C2SBlueprintPlacePayload.STREAM_CODEC,
                BlueprintNetworkHandlers::handlePlace);

        registrar.playToClient(
                S2CBlueprintStatusPayload.TYPE,
                S2CBlueprintStatusPayload.STREAM_CODEC,
                BlueprintClientPayloadBridge::handleStatus);
    }
}
