package com.rtsbuilding.rtsbuilding.client.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public abstract class RtsMovement {

    public static final double COST_INF = Double.MAX_VALUE / 2;
    public static final double WALK_ONE_BLOCK_COST = 1.0;
    public static final double JUMP_ONE_BLOCK_COST = 1.5;
    public static final double SNEAK_ONE_BLOCK_COST = 2.0;
    public static final double SPRINT_MULTIPLIER = 0.66;
    public static final double PLACE_BLOCK_COST = 5.0;

    protected final BlockPos src;
    protected final BlockPos dest;
    protected Double cost;

    public RtsMovement(BlockPos src, BlockPos dest) {
        this.src = src;
        this.dest = dest;
        this.cost = null;
    }

    public abstract double calculateCost(Level level);

    public double getCost(Level level) {
        if (cost == null) {
            cost = calculateCost(level);
        }
        return cost;
    }

    public BlockPos getSrc() {
        return src;
    }

    public BlockPos getDest() {
        return dest;
    }

    public void reset() {
        cost = null;
    }
}
