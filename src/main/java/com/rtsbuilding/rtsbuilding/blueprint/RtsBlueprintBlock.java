package com.rtsbuilding.rtsbuilding.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public record RtsBlueprintBlock(BlockPos relativePos, BlockState state, CompoundTag blockEntityTag) {
    public boolean hasBlockEntityTag() {
        return this.blockEntityTag != null && !this.blockEntityTag.isEmpty();
    }
}
