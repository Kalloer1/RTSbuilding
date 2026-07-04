package com.rtsbuilding.rtsbuilding.common;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RtsEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE, RtsbuildingMod.MODID);

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    private RtsEntities() {
    }
}