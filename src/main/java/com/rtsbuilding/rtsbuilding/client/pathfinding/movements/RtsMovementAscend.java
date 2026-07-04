package com.rtsbuilding.rtsbuilding.client.pathfinding.movements;

import com.rtsbuilding.rtsbuilding.client.pathfinding.RtsMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RtsMovementAscend extends RtsMovement {

    public RtsMovementAscend(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public double calculateCost(Level level) {
        BlockPos srcUp2 = src.above(2);

        if (!canWalkThrough(level, srcUp2)) {
            return COST_INF;
        }

        BlockState destState = level.getBlockState(dest);
        if (!canWalkOn(level, destState)) {
            return COST_INF;
        }

        double cost = Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST);

        if (!canWalkThrough(level, dest.above())) {
            return COST_INF;
        }

        return cost;
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

    private boolean isReplaceable(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        return level.getBlockState(pos).canBeReplaced();
    }

    private boolean canPlaceAgainst(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty();
    }
}
