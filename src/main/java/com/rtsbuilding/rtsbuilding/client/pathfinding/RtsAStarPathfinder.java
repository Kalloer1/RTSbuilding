package com.rtsbuilding.rtsbuilding.client.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;

public class RtsAStarPathfinder {

    private static final int MAX_SEARCH_STEPS = 20000;
    private static final int MAX_SEARCH_RANGE = 256;

    private final Level level;
    private final BlockPos start;
    private final BlockPos target;

    public RtsAStarPathfinder(Level level, BlockPos start, BlockPos target) {
        this.level = level;
        this.start = start;
        this.target = target;
    }

    public List<BlockPos> findPath() {
        if (start.equals(target)) {
            return Collections.emptyList();
        }

        int range = Math.max(
                Math.abs(start.getX() - target.getX()),
                Math.max(
                        Math.abs(start.getY() - target.getY()),
                        Math.abs(start.getZ() - target.getZ())));
        if (range > MAX_SEARCH_RANGE) {
            return Collections.emptyList();
        }

        PriorityQueue<RtsPathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.combinedCost));
        Map<BlockPos, RtsPathNode> allNodes = new HashMap<>();

        RtsPathNode startNode = new RtsPathNode(start.getX(), start.getY(), start.getZ(), target);
        startNode.cost = 0;
        startNode.combinedCost = startNode.estimatedCostToGoal;
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int steps = 0;
        RtsMoves[] allMoves = RtsMoves.values();

        while (!openSet.isEmpty() && steps < MAX_SEARCH_STEPS) {
            steps++;
            RtsPathNode current = openSet.poll();

            if (current.x == target.getX() && current.y == target.getY() && current.z == target.getZ()) {
                return reconstructPath(current);
            }

            for (RtsMoves move : allMoves) {
                int newX = current.x + move.xOffset;
                int newY = current.y + move.yOffset;
                int newZ = current.z + move.zOffset;

                if (!level.hasChunkAt(new BlockPos(newX, newY, newZ))) {
                    continue;
                }

                if (newY < level.getMinBuildHeight() || newY > level.getMaxBuildHeight()) {
                    continue;
                }

                double moveCost = move.cost(level, current.getPos());
                if (moveCost >= RtsPathNode.COST_INF) {
                    continue;
                }

                BlockPos newPos = new BlockPos(newX, newY, newZ);
                RtsPathNode neighbor = allNodes.computeIfAbsent(newPos,
                        pos -> new RtsPathNode(pos.getX(), pos.getY(), pos.getZ(), target));

                double tentativeCost = current.cost + moveCost;
                if (tentativeCost < neighbor.cost) {
                    neighbor.previous = current;
                    neighbor.cost = tentativeCost;
                    neighbor.combinedCost = tentativeCost + neighbor.estimatedCostToGoal;

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    private List<BlockPos> reconstructPath(RtsPathNode endNode) {
        List<BlockPos> path = new ArrayList<>();
        RtsPathNode current = endNode;

        while (current != null) {
            path.add(current.getPos());
            current = current.previous;
        }

        Collections.reverse(path);
        return path;
    }
}
