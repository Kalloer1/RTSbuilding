package com.rtsbuilding.rtsbuilding.network.builder.handler;

import com.rtsbuilding.rtsbuilding.network.builder.*;
import com.rtsbuilding.rtsbuilding.server.camera.RtsCameraManager;
import com.rtsbuilding.rtsbuilding.server.history.ServerHistoryManager;
import com.rtsbuilding.rtsbuilding.server.service.*;
import com.rtsbuilding.rtsbuilding.server.storage.RtsStorageSession;
import com.rtsbuilding.rtsbuilding.server.workflow.RtsWorkflowManager;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-side C2S adapter for RTS interaction, break, quick-drop, and undo
 * actions.
 *
 * <p>Keep interaction behavior, block recovery, and undo orchestration in
 * their respective services; this layer should only unwrap payloads and enqueue
 * work on the server thread.
 */
public final class RtsInteractionHandlers {
    private RtsInteractionHandlers() {
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

    public static void handleUndo(C2SRtsUndoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // Non-RTS mode undo requests are ignored
                if (!RtsCameraManager.isActive(serverPlayer)) return;
                ServerHistoryManager.executeUndo(serverPlayer);
            }
        });
    }

    public static void handleSubmitPending(C2SRtsSubmitPendingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                RtsPlacementService.submitPendingPlacement(serverPlayer);
            }
        });
    }

    public static void handleDeleteWorkflow(C2SRtsDeleteWorkflowPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                RtsStorageSession session = RtsSessionService.getIfPresent(serverPlayer);
                if (session != null) {
                    RtsWorkflowManager.deleteWorkflow(serverPlayer, session, payload.workflowIndex());
                }
            }
        });
    }

    public static void handleScanResumePlacement(C2SRtsScanResumePlacementPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                RtsStorageSession session = RtsSessionService.getIfPresent(serverPlayer);
                if (session != null) {
                    RtsResumeScanResult result = RtsPendingPlacementService.scanPendingJob(serverPlayer, session, payload.workflowEntryId());
                    if (result != null) {
                        PacketDistributor.sendToPlayer(serverPlayer, new S2CRtsResumePlacementScanPayload(
                                result.itemId(), result.itemLabel(),
                                result.totalRemaining(), result.alreadyPlacedCount(),
                                result.conflictCount(), result.availableItems(),
                                result.neededItems(), result.missingItems(),
                                result.workflowEntryId()));
                    }
                }
            }
        });
    }

    public static void handleResumePlacementAction(C2SRtsResumePlacementActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                RtsStorageSession session = RtsSessionService.getIfPresent(serverPlayer);
                if (session != null) {
                    RtsPendingPlacementService.resumeWithStrategy(serverPlayer, session, payload.strategy(), payload.workflowEntryId());
                }
            }
        });
    }
}
