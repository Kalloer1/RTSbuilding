package com.rtsbuilding.rtsbuilding.server.service.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 蓝图材料服务接口——管理蓝图所需的材料统计、提取、退还和页面刷新。
 *
 * <p>Phase 2 服务解耦的一部分。将 {@code RtsBlueprintService}
 * 的静态方法封装为实例方法，便于依赖注入和单元测试。
 */
public interface BlueprintService {

    /**
     * 统计指定物品在链接网络和玩家背包中的总量。
     */
    long countMaterial(ServerPlayer player, Item item);

    /**
     * 从链接网络提取指定物品。
     */
    ItemStack extractMaterial(ServerPlayer player, Item item, int count);

    /**
     * 统计指定流体在链接网络中的总量（mB）。
     */
    long countFluidMb(ServerPlayer player, Fluid fluid);

    /**
     * 从链接网络提取指定流体。
     */
    boolean extractFluid(ServerPlayer player, Fluid fluid, int amountMb);

    /**
     * 退还材料到链接存储或玩家背包。
     */
    void refundMaterial(ServerPlayer player, ItemStack stack);

    /**
     * 记录已放置的蓝图方块并播放音效。
     */
    void noteBlockPlaced(ServerPlayer player, BlockPos pos, String itemId);

    /**
     * 刷新蓝图对应的存储页面。
     */
    void refreshPage(ServerPlayer player);
}
