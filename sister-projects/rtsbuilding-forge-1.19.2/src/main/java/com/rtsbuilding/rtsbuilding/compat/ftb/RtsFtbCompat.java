package com.rtsbuilding.rtsbuilding.compat.ftb;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

import net.minecraft.server.level.ServerPlayer;
import com.rtsbuilding.rtsbuilding.forgecompat.fml.ModList;

public final class RtsFtbCompat {
    private static final boolean FTB_QUESTS_LOADED = ModList.get().isLoaded("ftbquests");
    private static final boolean FTB_TEAMS_LOADED = ModList.get().isLoaded("ftbteams");
    private static final RtsFtbCompatImpl IMPL = createImpl();

    private RtsFtbCompat() {
    }

    public static boolean isDetectAvailable() {
        return IMPL != null;
    }

    public static void detectNow(ServerPlayer player) {
        if (IMPL == null || player == null) {
            return;
        }
        IMPL.detectNow(player);
    }

    private static RtsFtbCompatImpl createImpl() {
        if (!FTB_QUESTS_LOADED || !FTB_TEAMS_LOADED) {
            return null;
        }
        try {
            return new RtsFtbCompatImpl();
        } catch (Throwable throwable) {
            RtsbuildingMod.LOGGER.warn("FTB compat init failed; quest detect disabled.", throwable);
            return null;
        }
    }
}

