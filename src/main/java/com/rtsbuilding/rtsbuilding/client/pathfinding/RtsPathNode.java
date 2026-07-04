package com.rtsbuilding.rtsbuilding.client.pathfinding;

import net.minecraft.core.BlockPos;

public class RtsPathNode {

    public final int x;
    public final int y;
    public final int z;

    public final double estimatedCostToGoal;
    public double cost;
    public double combinedCost;
    public RtsPathNode previous;
    public int heapPosition;

    public static final double COST_INF = Double.MAX_VALUE / 2;

    public RtsPathNode(int x, int y, int z, BlockPos target) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.cost = COST_INF;
        this.estimatedCostToGoal = heuristic(x, y, z, target);
        this.combinedCost = this.estimatedCostToGoal;
        this.previous = null;
        this.heapPosition = -1;
    }

    private static double heuristic(int x, int y, int z, BlockPos target) {
        double dx = Math.abs(x - target.getX());
        double dy = Math.abs(y - target.getY());
        double dz = Math.abs(z - target.getZ());
        double flat = Math.sqrt(dx * dx + dz * dz);
        return flat + dy * 0.5;
    }

    public boolean isOpen() {
        return heapPosition != -1;
    }

    public BlockPos getPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RtsPathNode)) return false;
        RtsPathNode other = (RtsPathNode) obj;
        return x == other.x && y == other.y && z == other.z;
    }
}
