package com.rtsbuilding.rtsbuilding.client.rendering.builder;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;

/**
 * Short client-only visual confirmation for server-approved RTS block changes.
 *
 * <p>This renderer owns only the transient animation list and line/fill drawing.
 * It does not decide whether placement/destruction succeeded, mutate world
 * state, or infer material usage. The server sends one payload after a real
 * action succeeds, and the client renders a small pulse around that block for a
 * few ticks.
 */
public final class PlacementAnimationRenderer {
    private static final long DURATION_MS = 320L;
    private static final int MAX_ACTIVE_ANIMATIONS = 1024;

    private PlacementAnimationRenderer() {
    }

    private static final Map<AnimationKey, PlacementPulse> PULSES = new LinkedHashMap<>();

    public static void add(BlockPos pos) {
        add(pos, PulseKind.PLACE);
    }

    public static void addDestroy(BlockPos pos) {
        add(pos, PulseKind.DESTROY);
    }

    private static void add(BlockPos pos, PulseKind kind) {
        if (pos == null) {
            return;
        }
        AnimationKey key = new AnimationKey(pos.asLong(), kind);
        PULSES.remove(key);
        PULSES.put(key, new PlacementPulse(pos.immutable(), kind, System.currentTimeMillis()));
        while (PULSES.size() > MAX_ACTIVE_ANIMATIONS) {
            Iterator<AnimationKey> iterator = PULSES.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
    }

    public static void render(Minecraft minecraft, PoseStack poseStack, VertexConsumer lineBuffer, VertexConsumer fillBuffer) {
        if (minecraft == null || minecraft.level == null || PULSES.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<PlacementPulse> iterator = PULSES.values().iterator();
        while (iterator.hasNext()) {
            PlacementPulse pulse = iterator.next();
            float progress = (now - pulse.startMs()) / (float) DURATION_MS;
            if (progress >= 1.0F) {
                iterator.remove();
                continue;
            }
            renderPulse(poseStack, lineBuffer, fillBuffer, pulse.pos(), pulse.kind(), progress);
        }
    }

    private static void renderPulse(PoseStack poseStack, VertexConsumer lineBuffer, VertexConsumer fillBuffer,
            BlockPos pos, PulseKind kind, float progress) {
        float fade = 1.0F - progress;
        double inset = kind == PulseKind.DESTROY
                ? 0.02D + easeOut(progress) * 0.34D
                : 0.015D + progress * 0.105D;
        double lift = kind == PulseKind.DESTROY ? 0.0D : progress * 0.06D;
        double minX = pos.getX() + inset;
        double minY = pos.getY() + inset + lift;
        double minZ = pos.getZ() + inset;
        double maxX = pos.getX() + 1.0D - inset;
        double maxY = pos.getY() + 1.0D - inset + lift;
        double maxZ = pos.getZ() + 1.0D - inset;

        float fillAlpha = (kind == PulseKind.DESTROY ? 0.14F : 0.18F) * fade;
        float lineAlpha = (kind == PulseKind.DESTROY ? 0.76F : 0.92F) * fade;
        float fillR = kind == PulseKind.DESTROY ? 1.00F : 0.32F;
        float fillG = kind == PulseKind.DESTROY ? 0.25F : 1.00F;
        float fillB = kind == PulseKind.DESTROY ? 0.44F : 0.48F;
        float lineR = kind == PulseKind.DESTROY ? 1.00F : 0.58F;
        float lineG = kind == PulseKind.DESTROY ? 0.46F : 1.00F;
        float lineB = kind == PulseKind.DESTROY ? 0.64F : 0.62F;
        LevelRenderer.addChainedFilledBoxVertices(
                poseStack,
                fillBuffer,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                fillR, fillG, fillB, fillAlpha);
        LevelRenderer.renderLineBox(
                poseStack,
                lineBuffer,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                lineR, lineG, lineB, lineAlpha);
    }

    private static float easeOut(float progress) {
        float inverse = 1.0F - progress;
        return 1.0F - inverse * inverse;
    }

    private enum PulseKind {
        PLACE,
        DESTROY
    }

    private record AnimationKey(long pos, PulseKind kind) {
    }

    private record PlacementPulse(BlockPos pos, PulseKind kind, long startMs) {
    }
}
