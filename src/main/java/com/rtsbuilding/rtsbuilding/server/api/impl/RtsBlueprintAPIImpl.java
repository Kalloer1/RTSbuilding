package com.rtsbuilding.rtsbuilding.server.api.impl;

import com.rtsbuilding.rtsbuilding.server.api.RtsBlueprintAPI;
import com.rtsbuilding.rtsbuilding.server.service.ServiceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public final class RtsBlueprintAPIImpl implements RtsBlueprintAPI {

    private static final ServiceRegistry REGISTRY = ServiceRegistry.getInstance();

    @Override
    public long countMaterial(ServerPlayer player, Item item) {
        return REGISTRY.blueprint().countMaterial(player, item);
    }

    @Override
    public ItemStack extractMaterial(ServerPlayer player, Item item, int count) {
        return REGISTRY.blueprint().extractMaterial(player, item, count);
    }

    @Override
    public long countFluidMb(ServerPlayer player, Fluid fluid) {
        return REGISTRY.blueprint().countFluidMb(player, fluid);
    }

    @Override
    public boolean extractFluid(ServerPlayer player, Fluid fluid, int amountMb) {
        return REGISTRY.blueprint().extractFluid(player, fluid, amountMb);
    }

    @Override
    public void refundMaterial(ServerPlayer player, ItemStack stack) {
        REGISTRY.blueprint().refundMaterial(player, stack);
    }

    @Override
    public void noteBlockPlaced(ServerPlayer player, Object pos, String itemId) {
        if (pos instanceof BlockPos bp) {
            REGISTRY.blueprint().noteBlockPlaced(player, bp, itemId);
        }
    }

    @Override
    public void refreshPage(ServerPlayer player) {
        REGISTRY.blueprint().refreshPage(player);
    }
}
