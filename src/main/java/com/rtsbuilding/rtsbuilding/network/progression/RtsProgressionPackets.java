package com.rtsbuilding.rtsbuilding.network.progression;

import com.rtsbuilding.rtsbuilding.network.ClientPayloadDispatcher;
import com.rtsbuilding.rtsbuilding.network.progression.handler.RtsProgressionNetworkHandlers;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers quest-detect and RTS-home packets.
 *
 * <p>All packets use the optional registrar to allow clients without the mod
 * to connect to the server.</p>
 */
public final class RtsProgressionPackets {
    private RtsProgressionPackets() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
                C2SRtsQuestDetectPayload.TYPE,
                C2SRtsQuestDetectPayload.STREAM_CODEC,
                RtsProgressionNetworkHandlers::handleQuestDetect);

        registrar.playToServer(
                C2SRtsSetSurvivalProgressionPayload.TYPE,
                C2SRtsSetSurvivalProgressionPayload.STREAM_CODEC,
                RtsProgressionNetworkHandlers::handleSetSurvivalProgression);

        registrar.playToServer(
                C2SRtsSetHomePayload.TYPE,
                C2SRtsSetHomePayload.STREAM_CODEC,
                RtsProgressionNetworkHandlers::handleSetHome);

        registrar.playToServer(
                C2SRtsBeginHomeSelectionPayload.TYPE,
                C2SRtsBeginHomeSelectionPayload.STREAM_CODEC,
                RtsProgressionNetworkHandlers::handleBeginHomeSelection);

        registrar.playToServer(
                C2SRtsRequestProgressionStatePayload.TYPE,
                C2SRtsRequestProgressionStatePayload.STREAM_CODEC,
                RtsProgressionNetworkHandlers::handleRequestProgressionState);

        registrar.playToClient(
                S2CRtsQuestDetectStatusPayload.TYPE,
                S2CRtsQuestDetectStatusPayload.STREAM_CODEC,
                ClientPayloadDispatcher::dispatchProgression);

        registrar.playToClient(
                S2CRtsProgressionStatePayload.TYPE,
                S2CRtsProgressionStatePayload.STREAM_CODEC,
                ClientPayloadDispatcher::dispatchProgression);
    }
}
