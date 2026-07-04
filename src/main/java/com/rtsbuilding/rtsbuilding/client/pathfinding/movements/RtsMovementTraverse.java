package com.rtsbuilding.rtsbuilding.client.pathfinding.movements;

import com.rtsbuilding.rtsbuilding.client.pathfinding.RtsMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RtsMovementTraverse extends RtsMovement {

    public RtsMovementTraverse(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public double calculateCost(Level level) {
        BlockState destAbove = level.getBlockState(dest.above());
        BlockState destState = level.getBlockState(dest);
        BlockState destBelow = level.getBlockState(dest.below());

        if (!canWalkThrough(level, dest.above())) {
            return COST_INF;
        }
        if (!canWalkThrough(level, dest)) {
            return COST_INF;
        }
        if (!canWalkOn(level, dest.below())) {
            return COST_INF;
        }

        double cost = WALK_ONE_BLOCK_COST;

        if (isWater(level, dest.above()) || isWater(level, dest)) {
            cost *= 2.0;
        }

        if (destBelow.is(Blocks.SOUL_SAND)) {
            cost += 0.5;
        }

        return cost * SPRINT_MULTIPLIER;
    }

    private boolean canWalkThrough(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() || shape.max(Direction.Axis.Y) < 0.8;
    }

    private boolean canWalkOn(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 0.5;
    }

    private boolean isWater(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        return level.getBlockState(pos).getFluidState().is(FluidTags.WATER);
    }
}
