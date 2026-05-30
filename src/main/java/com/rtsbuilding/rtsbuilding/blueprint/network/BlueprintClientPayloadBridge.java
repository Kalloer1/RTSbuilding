package com.rtsbuilding.rtsbuilding.blueprint.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class BlueprintClientPayloadBridge {
    private BlueprintClientPayloadBridge() {
    }

    public static void handleStatus(S2CBlueprintStatusPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.rtsbuilding.rtsbuilding.blueprint.client.BlueprintClientNetworkHandlers.handleStatus(payload, context);
        }
    }
}
