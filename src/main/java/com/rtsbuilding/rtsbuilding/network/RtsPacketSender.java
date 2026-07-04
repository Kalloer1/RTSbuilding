package com.rtsbuilding.rtsbuilding.network;

import com.rtsbuilding.rtsbuilding.server.RtsClientModTracker;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RtsPacketSender {

    private RtsPacketSender() {
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        if (!RtsClientModTracker.clientHasMod(player)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
    }
}