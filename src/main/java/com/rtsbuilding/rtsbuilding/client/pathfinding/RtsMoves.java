package com.rtsbuilding.rtsbuilding.client.pathfinding;

import com.rtsbuilding.rtsbuilding.client.pathfinding.movements.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public enum RtsMoves {

    DOWNWARD(0, -1, 0) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementFall(src, src.below());
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementFall(pos, pos.below()).calculateCost(level);
        }
    },

    TRAVERSE_NORTH(0, 0, -1) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementTraverse(src, src.north());
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementTraverse(pos, pos.north()).calculateCost(level);
        }
    },

    TRAVERSE_SOUTH(0, 0, +1) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementTraverse(src, src.south());
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementTraverse(pos, pos.south()).calculateCost(level);
        }
    },

    TRAVERSE_EAST(+1, 0, 0) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementTraverse(src, src.east());
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementTraverse(pos, pos.east()).calculateCost(level);
        }
    },

    TRAVERSE_WEST(-1, 0, 0) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementTraverse(src, src.west());
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementTraverse(pos, pos.west()).calculateCost(level);
        }
    },

    ASCEND_NORTH(0, +1, -1) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementAscend(src, new BlockPos(src.getX(), src.getY() + 1, src.getZ() - 1));
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementAscend(pos, new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ() - 1)).calculateCost(level);
        }
    },

    ASCEND_SOUTH(0, +1, +1) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementAscend(src, new BlockPos(src.getX(), src.getY() + 1, src.getZ() + 1));
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementAscend(pos, new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ() + 1)).calculateCost(level);
        }
    },

    ASCEND_EAST(+1, +1, 0) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementAscend(src, new BlockPos(src.getX() + 1, src.getY() + 1, src.getZ()));
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementAscend(pos, new BlockPos(pos.getX() + 1, pos.getY() + 1, pos.getZ())).calculateCost(level);
        }
    },

    ASCEND_WEST(-1, +1, 0) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementAscend(src, new BlockPos(src.getX() - 1, src.getY() + 1, src.getZ()));
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementAscend(pos, new BlockPos(pos.getX() - 1, pos.getY() + 1, pos.getZ())).calculateCost(level);
        }
    },

    JUMP_UP(0, +1, 0) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementJumpUp(src, src.above());
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementJumpUp(pos, pos.above()).calculateCost(level);
        }
    },

    DESCEND_NORTH(0, -1, -1) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementDescend(src, new BlockPos(src.getX(), src.getY() - 1, src.getZ() - 1));
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementDescend(pos, new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ() - 1)).calculateCost(level);
        }
    },

    DESCEND_SOUTH(0, -1, +1) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementDescend(src, new BlockPos(src.getX(), src.getY() - 1, src.getZ() + 1));
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementDescend(pos, new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ() + 1)).calculateCost(level);
        }
    },

    DESCEND_EAST(+1, -1, 0) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementDescend(src, new BlockPos(src.getX() + 1, src.getY() - 1, src.getZ()));
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementDescend(pos, new BlockPos(pos.getX() + 1, pos.getY() - 1, pos.getZ())).calculateCost(level);
        }
    },

    DESCEND_WEST(-1, -1, 0) {
        @Override
        public RtsMovement create(BlockPos src) {
            return new RtsMovementDescend(src, new BlockPos(src.getX() - 1, src.getY() - 1, src.getZ()));
        }

        @Override
        public double cost(Level level, BlockPos pos) {
            return new RtsMovementDescend(pos, new BlockPos(pos.getX() - 1, pos.getY() - 1, pos.getZ())).calculateCost(level);
        }
    };

    public final int xOffset;
    public final int yOffset;
    public final int zOffset;

    RtsMoves(int x, int y, int z) {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;
    }

    public abstract RtsMovement create(BlockPos src);

    public abstract double cost(Level level, BlockPos pos);
}
