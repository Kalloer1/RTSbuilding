package com.rtsbuilding.rtsbuilding.client.pathfinding.movements;

import com.rtsbuilding.rtsbuilding.client.pathfinding.RtsMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RtsMovementFall extends RtsMovement {

    private static final int MAX_FALL_HEIGHT = 3;

    public RtsMovementFall(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public double calculateCost(Level level) {
        int fallHeight = src.getY() - dest.getY();
        if (fallHeight < 1 || fallHeight > MAX_FALL_HEIGHT) {
            return COST_INF;
        }

        for (int y = src.getY() - 1; y >= dest.getY(); y--) {
            BlockPos pos = new BlockPos(src.getX(), y, src.getZ());
            if (!canWalkThrough(level, pos)) {
                return COST_INF;
            }
        }

        BlockState destBelow = level.getBlockState(dest.below());
        if (!canWalkOn(level, destBelow)) {
            return COST_INF;
        }

        return fallHeight * 0.2;
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
