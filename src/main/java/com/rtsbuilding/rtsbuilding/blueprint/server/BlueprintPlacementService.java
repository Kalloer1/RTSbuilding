package com.rtsbuilding.rtsbuilding.blueprint.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.rtsbuilding.rtsbuilding.Config;
import com.rtsbuilding.rtsbuilding.blueprint.RtsBlueprint;
import com.rtsbuilding.rtsbuilding.blueprint.RtsBlueprintBlock;
import com.rtsbuilding.rtsbuilding.blueprint.network.BlueprintNetworkHandlers;
import com.rtsbuilding.rtsbuilding.blueprint.network.S2CBlueprintStatusPayload;
import com.rtsbuilding.rtsbuilding.server.RtsStorageManager;
import com.rtsbuilding.rtsbuilding.server.data.PlacedBlockTrackerData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public final class BlueprintPlacementService {
    private static final int MAX_BLOCKS = 8192;
    private static final int BLOCKS_PER_TICK = 64;
    private static final Map<UUID, PlacementJob> JOBS = new ConcurrentHashMap<>();

    private BlueprintPlacementService() {
    }

    public static void queuePlacement(ServerPlayer player, RtsBlueprint blueprint, BlockPos anchor, byte rotationSteps) {
        if (player == null || blueprint == null || anchor == null) {
            return;
        }
        if (!Config.areBlueprintsEnabled()) {
            send(player, S2CBlueprintStatusPayload.ERROR, "screen.rtsbuilding.blueprints.status.disabled", "");
            return;
        }
        if (blueprint.blocks().isEmpty()) {
            send(player, S2CBlueprintStatusPayload.ERROR, "screen.rtsbuilding.blueprints.status.empty", "");
            return;
        }
        if (blueprint.blockCount() > MAX_BLOCKS) {
            send(player, S2CBlueprintStatusPayload.ERROR, "screen.rtsbuilding.blueprints.status.too_many_blocks",
                    Integer.toString(blueprint.blockCount()));
            return;
        }

        PreflightResult preflight = preflight(player, blueprint, anchor, rotationSteps);
        if (!preflight.ok()) {
            send(player, S2CBlueprintStatusPayload.ERROR, preflight.messageKey(), preflight.detail());
            return;
        }

        JOBS.put(player.getUUID(), new PlacementJob(blueprint, anchor.immutable(), normalizeRotationSteps(rotationSteps), 0));
        send(player, S2CBlueprintStatusPayload.INFO, "screen.rtsbuilding.blueprints.status.queued",
                Integer.toString(blueprint.blockCount()));
    }

    public static void tick(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlacementJob job = JOBS.get(player.getUUID());
        if (job == null) {
            return;
        }
        if (!Config.areBlueprintsEnabled()) {
            abort(player, "screen.rtsbuilding.blueprints.status.disabled", "");
            return;
        }

        ServerLevel level = player.serverLevel();
        Rotation rotation = rotationForSteps(job.rotationSteps());
        int placed = 0;
        int index = job.nextIndex();
        while (index < job.blueprint().blocks().size() && placed < BLOCKS_PER_TICK) {
            RtsBlueprintBlock block = job.blueprint().blocks().get(index);
            BlockPos target = job.anchor().offset(rotate(block.relativePos(), job.rotationSteps()));
            if (!canStillPlace(player, level, target)) {
                abort(player, "screen.rtsbuilding.blueprints.status.blocked", shortPos(target));
                return;
            }

            BlockState state = block.state().rotate(rotation);
            Item item = state.getBlock().asItem();
            if (item == Items.AIR) {
                abort(player, "screen.rtsbuilding.blueprints.status.unsupported", state.getBlock().getName().getString());
                return;
            }

            ItemStack extracted = player.isCreative()
                    ? new ItemStack(item)
                    : RtsStorageManager.extractBlueprintMaterial(player, item, 1);
            if (extracted.isEmpty()) {
                abort(player, "screen.rtsbuilding.blueprints.status.missing", item.getDescription().getString());
                return;
            }

            boolean placedBlock = level.setBlock(target, state, 3);
            if (!placedBlock) {
                if (!player.isCreative()) {
                    RtsStorageManager.refundBlueprintMaterial(player, extracted);
                }
                abort(player, "screen.rtsbuilding.blueprints.status.blocked", shortPos(target));
                return;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            PlacedBlockTrackerData.get(level).mark(target);
            RtsStorageManager.noteBlueprintBlockPlaced(player, target, itemId == null ? "" : itemId.toString());
            index++;
            placed++;
        }

        if (index >= job.blueprint().blocks().size()) {
            JOBS.remove(player.getUUID());
            RtsStorageManager.refreshBlueprintStoragePage(player);
            send(player, S2CBlueprintStatusPayload.SUCCESS, "screen.rtsbuilding.blueprints.status.complete",
                    Integer.toString(job.blueprint().blockCount()));
        } else {
            JOBS.put(player.getUUID(), job.withNextIndex(index));
        }
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            JOBS.remove(player.getUUID());
        }
    }

    private static PreflightResult preflight(ServerPlayer player, RtsBlueprint blueprint, BlockPos anchor, byte rotationSteps) {
        ServerLevel level = player.serverLevel();
        int steps = normalizeRotationSteps(rotationSteps);
        Map<Item, Integer> required = new HashMap<>();
        for (RtsBlueprintBlock block : blueprint.blocks()) {
            BlockPos target = anchor.offset(rotate(block.relativePos(), steps));
            if (!canStillPlace(player, level, target)) {
                return PreflightResult.error("screen.rtsbuilding.blueprints.status.blocked", shortPos(target));
            }
            Item item = block.state().getBlock().asItem();
            if (item == Items.AIR) {
                return PreflightResult.error(
                        "screen.rtsbuilding.blueprints.status.unsupported",
                        block.state().getBlock().getName().getString());
            }
            required.merge(item, 1, Integer::sum);
        }

        if (!player.isCreative()) {
            for (Map.Entry<Item, Integer> entry : required.entrySet()) {
                long available = RtsStorageManager.countBlueprintMaterial(player, entry.getKey());
                if (available < entry.getValue()) {
                    return PreflightResult.error(
                            "screen.rtsbuilding.blueprints.status.missing",
                            entry.getKey().getDescription().getString() + " " + available + "/" + entry.getValue());
                }
            }
        }
        return PreflightResult.success();
    }

    private static boolean canStillPlace(ServerPlayer player, ServerLevel level, BlockPos target) {
        if (!RtsStorageManager.canAccessBlueprintTarget(player, target)) {
            return false;
        }
        if (level.getBlockEntity(target) != null) {
            return false;
        }
        return level.getBlockState(target).canBeReplaced();
    }

    private static int normalizeRotationSteps(byte steps) {
        return Math.floorMod(steps, 4);
    }

    private static Rotation rotationForSteps(int steps) {
        return switch (Math.floorMod(steps, 4)) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static BlockPos rotate(BlockPos pos, int steps) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return switch (Math.floorMod(steps, 4)) {
            case 1 -> new BlockPos(-z, y, x);
            case 2 -> new BlockPos(-x, y, -z);
            case 3 -> new BlockPos(z, y, -x);
            default -> pos;
        };
    }

    private static void abort(ServerPlayer player, String messageKey, String detail) {
        JOBS.remove(player.getUUID());
        RtsStorageManager.refreshBlueprintStoragePage(player);
        send(player, S2CBlueprintStatusPayload.ERROR, messageKey, detail);
    }

    private static void send(ServerPlayer player, byte status, String messageKey, String detail) {
        BlueprintNetworkHandlers.send(player, status, messageKey, detail);
    }

    private static String shortPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private record PlacementJob(RtsBlueprint blueprint, BlockPos anchor, int rotationSteps, int nextIndex) {
        PlacementJob withNextIndex(int nextIndex) {
            return new PlacementJob(this.blueprint, this.anchor, this.rotationSteps, nextIndex);
        }
    }

    private record PreflightResult(boolean ok, String messageKey, String detail) {
        static PreflightResult success() {
            return new PreflightResult(true, "", "");
        }

        static PreflightResult error(String messageKey, String detail) {
            return new PreflightResult(false, messageKey, detail == null ? "" : detail);
        }
    }
}
