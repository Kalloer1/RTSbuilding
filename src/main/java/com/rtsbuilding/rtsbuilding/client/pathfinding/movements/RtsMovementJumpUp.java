package com.rtsbuilding.rtsbuilding.client.pathfinding.movements;

import com.rtsbuilding.rtsbuilding.client.pathfinding.RtsMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RtsMovementJumpUp extends RtsMovement {

    public RtsMovementJumpUp(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public double calculateCost(Level level) {
        BlockPos srcUp = src.above();
        BlockPos srcUp2 = src.above(2);
        BlockPos destAbove = dest.above();

        if (!canWalkThrough(level, srcUp)) {
            return COST_INF;
        }

        if (!canWalkThrough(level, srcUp2)) {
            return COST_INF;
        }

        BlockState destState = level.getBlockState(dest);
        if (!canWalkOn(level, destState)) {
            return COST_INF;
        }

        if (!canWalkThrough(level, destAbove)) {
            return COST_INF;
        }

        return JUMP_ONE_BLOCK_COST;
    }

    private boolean canWalkThrough(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() || shape.max(Direction.Axis.Y) < 0.8;
    }

    private boolean canWalkOn(Level level, BlockState state) {
        return !state.isAir();
    }
}
