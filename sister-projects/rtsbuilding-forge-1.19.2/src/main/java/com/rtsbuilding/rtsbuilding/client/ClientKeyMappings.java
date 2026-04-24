package com.rtsbuilding.rtsbuilding.client;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RtsbuildingMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientKeyMappings {
    public static final KeyMapping TOGGLE_RTS = new KeyMapping(
            "key.rtsbuilding.toggle_rts",
            GLFW.GLFW_KEY_R,
            "key.categories.rtsbuilding");

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(final RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_RTS);
    }
}
