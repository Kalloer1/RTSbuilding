package com.rtsbuilding.rtsbuilding.client;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = RtsbuildingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientKeyMappings {
    public static final KeyMapping TOGGLE_RTS = new KeyMapping(
            "key.rtsbuilding.toggle_rts",
            GLFW.GLFW_KEY_R,
            "key.categories.rtsbuilding");

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_RTS);
    }
}
