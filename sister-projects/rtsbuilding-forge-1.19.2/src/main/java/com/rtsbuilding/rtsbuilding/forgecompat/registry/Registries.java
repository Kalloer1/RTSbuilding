package com.rtsbuilding.rtsbuilding.forgecompat.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class Registries {
    public static final ResourceKey<Registry<Level>> DIMENSION = Registry.DIMENSION_REGISTRY;

    private Registries() {
    }
}
