package com.rtsbuilding.rtsbuilding.common.shape.generator;

import com.rtsbuilding.rtsbuilding.common.shape.model.AreaShapeInput;
import com.rtsbuilding.rtsbuilding.common.shape.model.ShapeFillMode;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 长方体（BOX / 3D 立方体）形状生成器。
 * <p>
 * 生成由两个对角点和可选高度偏移定义的长方体内的所有方块位置。
 * 支持 FILL（实心）、HOLLOW（空心）和 SKELETON（骨架）三种填充模式。
 */
public class BoxShapeGenerator extends AreaShapeGenerator {

    @Override
    public String getName() {
        return "box";
    }

    @Override
    public List<BlockPos> generatePositions(AreaShapeInput input, ShapeFillMode fillMode) {
        int dx = clampOffset(input.end().getX() - input.start().getX());
        int dz = clampOffset(input.end().getZ() - input.start().getZ());
        int dy = clampOffset(input.heightOffset());

        int minX = Math.min(0, dx);
        int maxX = Math.max(0, dx);
        int minZ = Math.min(0, dz);
        int maxZ = Math.max(0, dz);
        int minY = Math.min(0, dy);
        int maxY = Math.max(0, dy);

        if (fillMode == ShapeFillMode.SKELETON) {
            return generateSkeletonPositions(input.start(), minX, maxX, minY, maxY, minZ, maxZ);
        }

        List<BlockPos> full = new ArrayList<>();
        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    full.add(input.start().offset(x, y, z));
                }
            }
        }

        if (fillMode == ShapeFillMode.FILL || full.isEmpty()) {
            return full;
        }

        return filterBoundary(full, minY, maxY);
    }

    private static List<BlockPos> generateSkeletonPositions(BlockPos start, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        Set<BlockPos> skeleton = new HashSet<>();

        // 顶面4条棱线
        for (int x = minX; x <= maxX; x++) {
            skeleton.add(start.offset(x, maxY, minZ));
            skeleton.add(start.offset(x, maxY, maxZ));
        }
        for (int z = minZ; z <= maxZ; z++) {
            skeleton.add(start.offset(minX, maxY, z));
            skeleton.add(start.offset(maxX, maxY, z));
        }

        // 底面4条棱线
        for (int x = minX; x <= maxX; x++) {
            skeleton.add(start.offset(x, minY, minZ));
            skeleton.add(start.offset(x, minY, maxZ));
        }
        for (int z = minZ; z <= maxZ; z++) {
            skeleton.add(start.offset(minX, minY, z));
            skeleton.add(start.offset(maxX, minY, z));
        }

        // 竖边4条棱线
        for (int y = minY; y <= maxY; y++) {
            skeleton.add(start.offset(minX, y, minZ));
            skeleton.add(start.offset(maxX, y, minZ));
            skeleton.add(start.offset(maxX, y, maxZ));
            skeleton.add(start.offset(minX, y, maxZ));
        }

        return new ArrayList<>(skeleton);
    }
}