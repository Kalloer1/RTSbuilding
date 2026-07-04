package com.rtsbuilding.rtsbuilding.client.compat.jade;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import net.minecraft.resources.ResourceLocation;

public class RtsJadePlugin {

    public static final ResourceLocation SHOW_RTS_INFO = ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "show_rts_info");

    public static void init() {
        try {
            Class.forName("snownee.jade.overlay.WailaTickHandler");
            RtsbuildingMod.LOGGER.info("RTS Jade compatibility: Jade detected, Mixin will handle rendering");
        } catch (ClassNotFoundException e) {
            RtsbuildingMod.LOGGER.info("RTS Jade compatibility: Jade not found");
        }
    }
}
