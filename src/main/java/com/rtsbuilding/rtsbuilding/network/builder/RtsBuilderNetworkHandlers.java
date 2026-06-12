package com.rtsbuilding.rtsbuilding.network.builder;

import com.rtsbuilding.rtsbuilding.common.BuilderMode;
import com.rtsbuilding.rtsbuilding.server.camera.RtsCameraManager;
import com.rtsbuilding.rtsbuilding.server.history.ServerHistoryManager;
import com.rtsbuilding.rtsbuilding.server.service.RtsBindingService;
import com.rtsbuilding.rtsbuilding.server.service.RtsFluidService;
import com.rtsbuilding.rtsbuilding.server.service.RtsInteractionService;
import com.rtsbuilding.rtsbuilding.server.service.RtsMiningService;
import com.rtsbuilding.rtsbuilding.server.service.RtsPlacementService;
import com.rtsbuilding.rtsbuilding.server.service.RtsPlacedRecoveryService;
import com.rtsbuilding.rtsbuilding.server.service.RtsTransferService;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-side C2S adapter for placement, mining, and world interaction actions.
 *
 * Keep gameplay validation, item extraction, tool leasing, and undo/recent
 * updates in RtsStorageManager; this layer should only unwrap payloads and
 * enqueue work on the server thread.
 */
public final class RtsBuilderNetworkHandlers {
    private RtsBuilderNetworkHandlers() {
    }

    public static void handleSetMode(C2SRtsSetModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                int modeId = payload.mode();
                var modes = BuilderMode.values();
                if (modeId < 0 || modeId >= modes.length) {
                    return;
                }
                RtsBindingService.setMode(serverPlayer, modes[modeId]);
            }
        });
    }

    public static void handleRotateBlock(C2SRtsRotateBlockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                RtsPlacementService.rotateBlock(serverPlayer, payload.pos());
            }
        });
    }

    public static void handlePlace(C2SRtsPlacePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Direction face = Direction.from3DDataValue(payload.face());
                RtsPlacementService.placeSelected(
                        serverPlayer,
                        payload.clickedPos(),
                        face,
                        payload.hitX(),
                        payload.hitY(),
                        payload.hitZ(),
                        payload.rotateSteps(),
                        payload.forcePlace(),
                        payload.skipIfOccupied(),
                        payload.itemId(),
                        payload.itemPrototype(),
                        payload.rayOriginX(),
                        payload.rayOriginY(),
                        payload.rayOriginZ(),
                        payload.rayDirX(),
                        payload.rayDirY(),
                        payload.rayDirZ(),
                        payload.quickBuild(),
                        payload.forceEmptyHand());
            }
        });
    }

    public static void handlePlaceBatch(C2SRtsPlaceBatchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Direction face = Direction.from3DDataValue(payload.face());
                RtsPlacementService.enqueuePlaceBatch(
                        serverPlayer,
                        payload.clickedPositions(),
                        face,
                        payload.hitOffsetX(),
                        payload.hitOffsetY(),
                        payload.hitOffsetZ(),
                        payload.rotateSteps(),
                        payload.forcePlace(),
                        payload.skipIfOccupied(),
                        payload.itemId(),
                        payload.itemPrototype(),
                        payload.rayOriginX(),
                        payload.rayOriginY(),
                        payload.rayOriginZ(),
                        payload.rayDirX(),
                        payload.rayDirY(),
                        payload.rayDirZ());
            }
        });
    }

    public static void handlePlaceFluid(C2SRtsPlaceFluidPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Direction face = Direction.from3DDataValue(payload.face());
                RtsFluidService.placeFluid(
                        serverPlayer,
                        payload.clickedPos(),
                        face,
                        payload.hitX(),
                        payload.hitY(),
                        payload.hitZ(),
                        payload.forcePlace(),
                        payload.fluidId(),
                        payload.rayOriginX(),
                        payload.rayOriginY(),
                        payload.rayOriginZ(),
                        payload.rayDirX(),
                        payload.rayDirY(),
                        payload.rayDirZ());
            }
        });
    }

    public static void handleStoreFluid(C2SRtsStoreFluidPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                RtsFluidService.storeFluidFromContainer(
                        serverPlayer,
                        payload.sourceType(),
                        payload.toolSlot(),
                        payload.itemId());
            }
        });
    }

    public static void handleInteract(C2SRtsInteractPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Direction face = Direction.from3DDataValue(payload.face());
                RtsInteractionService.interactTarget(
                        serverPlayer,
                        payload.entityId(),
                        payload.clickedPos(),
                        face,
                        payload.hitX(),
                        payload.hitY(),
                        payload.hitZ(),
                        payload.sourceType(),
                        payload.toolSlot(),
                        payload.itemId(),
                        payload.rayOriginX(),
                        payload.rayOriginY(),
                        payload.rayOriginZ(),
                        payload.rayDirX(),
                        payload.rayDirY(),
                        payload.rayDirZ());
            }
        });
    }

    public static void handleQuickDrop(C2SRtsQuickDropPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                RtsTransferService.quickDropLinkedItem(
                        serverPlayer,
                        payload.itemId(),
                        payload.amount(),
                        payload.dropX(),
                        payload.dropY(),
                        payload.dropZ());
            }
        });
    }

    public static void handleBreak(C2SRtsBreakPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Direction face = Direction.from3DDataValue(payload.face());
                RtsPlacedRecoveryService.breakPlaced(serverPlayer, payload.pos(), face, payload.allowAdjacentFallback());
            }
        });
    }

    public static void handleMine(C2SRtsMinePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Direction face = Direction.from3DDataValue(payload.face());
                RtsMiningService.mine(
                        serverPlayer,
                        payload.pos(),
                        face,
                        payload.start(),
                        payload.toolSlot(),
                        payload.toolItemId(),
                        payload.toolPrototype(),
                        payload.allowPlacedBlockRecovery(),
                        payload.toolProtectionEnabled());
            }
        });
    }

    public static void handleUltimine(C2SRtsUltiminePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Direction face = Direction.from3DDataValue(payload.face());
                RtsMiningService.startUltimine(
                        serverPlayer,
                        payload.pos(),
                        face,
                        payload.toolSlot(),
                        payload.toolItemId(),
                        payload.toolPrototype(),
                        payload.limit(),
                        payload.mode(),
                        payload.toolProtectionEnabled());
            }
        });
    }

    public static void handleAreaMine(C2SRtsAreaMinePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                RtsMiningService.areaMine(
                        serverPlayer,
                        payload.minX(), payload.maxX(),
                        payload.minY(), payload.maxY(),
                        payload.minZ(), payload.maxZ(),
                        payload.toolSlot(),
                        payload.toolItemId(),
                        payload.toolPrototype(),
                        payload.shapeType(),
                        payload.fillType(),
                        payload.toolProtectionEnabled());
            }
        });
    }

    public static void handleAreaDestroy(C2SRtsAreaDestroyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                RtsMiningService.areaDestroy(
                        serverPlayer,
                        payload.positions(),
                        payload.toolSlot(),
                        payload.toolItemId(),
                        payload.toolPrototype(),
                        payload.toolProtectionEnabled());
            }
        });
    }

    // ======================================================================
    //  Undo （基于 Ultimine-Rewind 风格的服务端管理）
    //  完整流程由 ServerHistoryManager.executeUndo 处理，
    //  包括出栈、执行、部分恢复和状态同步。
    // ======================================================================

    public static void handleUndo(C2SRtsUndoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // 非 RTS 模式下忽略撤回请求
                if (!RtsCameraManager.isActive(serverPlayer)) return;
                ServerHistoryManager.executeUndo(serverPlayer);
            }
        });
    }
}
