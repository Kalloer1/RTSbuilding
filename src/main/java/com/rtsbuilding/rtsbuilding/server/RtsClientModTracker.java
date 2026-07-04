package com.rtsbuilding.rtsbuilding.server;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RtsClientModTracker {

    private static final Set<UUID> clientsWithMod = ConcurrentHashMap.newKeySet();

    private RtsClientModTracker() {
    }

    public static void markClientHasMod(ServerPlayer player) {
        clientsWithMod.add(player.getUUID());
    }

    public static boolean clientHasMod(ServerPlayer player) {
        return clientsWithMod.contains(player.getUUID());
    }

    public static void removeClient(ServerPlayer player) {
        clientsWithMod.remove(player.getUUID());
    }
}