package com.rtsbuilding.rtsbuilding.client.pathfinding.movements;

import com.rtsbuilding.rtsbuilding.client.pathfinding.RtsMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RtsMovementDescend extends RtsMovement {

    public RtsMovementDescend(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public double calculateCost(Level level) {
        if (!canWalkThrough(level, dest)) {
            return COST_INF;
        }
        if (!canWalkThrough(level, dest.above())) {
            return COST_INF;
        }

        BlockState destBelow = level.getBlockState(dest.below());
        if (!canWalkOn(level, destBelow)) {
            return COST_INF;
        }

        return WALK_ONE_BLOCK_COST * 0.5;
    }

    private boolean canWalkThrough(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() || shape.max(Direction.Axis.Y) < 0.8;
    }

    private boolean canWalkOn(Level level, BlockState state) {
        VoxelShape shape = state.getCollisionShape(level, BlockPos.ZERO);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 0.5;
    }
}
