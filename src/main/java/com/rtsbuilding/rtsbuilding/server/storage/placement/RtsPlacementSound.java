package com.rtsbuilding.rtsbuilding.server.storage.placement;

import com.rtsbuilding.rtsbuilding.network.builder.S2CRtsPlaceAnimationPayload;
import com.rtsbuilding.rtsbuilding.server.RtsStorageManager;
import com.rtsbuilding.rtsbuilding.server.storage.RtsStorageSession;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sound and animation effects for RTS remote block placement.
 *
 * <p>This helper owns only auditory and visual feedback emitted after a
 * remote placement succeeds: per-block break/replace sounds, per-block
 * animation packets, and a quick-build completion chime. It deliberately
 * does not execute placement, extract items, or manage batch jobs.
 */
public final class RtsPlacementSound {
    private static final int QUICK_BUILD_COMPLETION_SOUND_DELAY_TICKS = 3;

    private RtsPlacementSound() {
    }

    /**
     * Sends a block-break animation packet to the player for the given
     * position.
     */
    public static void playRemotePlacedBlockAnimation(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new S2CRtsPlaceAnimationPayload(pos.immutable()));
    }

    /**
     * Plays the block-place sound for a remotely placed block, suppressing
     * duplicate ticks during quick-build so that only one place sound is
     * audible per game tick.
     */
    public static void playRemotePlacedBlockSound(ServerPlayer player, ServerLevel level, RtsStorageSession session,
                                                   BlockPos pos, boolean quickBuild) {
        if (player == null || level == null || pos == null || !level.hasChunkAt(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (quickBuild && session != null) {
            noteQuickBuildPlacement(session, pos, gameTime);
            if (session.lastQuickBuildPlaceSoundTick == gameTime) {
                return;
            }
            session.lastQuickBuildPlaceSoundTick = gameTime;
        }
        SoundType soundType = state.getSoundType(level, pos, player);
        RtsStorageManager.sendDirectSound(
                player,
                soundType.getPlaceSound(),
                SoundSource.BLOCKS,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F);
    }

    private static void noteQuickBuildPlacement(RtsStorageSession session, BlockPos pos, long gameTime) {
        session.quickBuildSoundPlacedCount++;
        session.quickBuildCompletionSoundTick = gameTime + QUICK_BUILD_COMPLETION_SOUND_DELAY_TICKS;
        session.quickBuildSoundX = pos.getX() + 0.5D;
        session.quickBuildSoundY = pos.getY() + 0.5D;
        session.quickBuildSoundZ = pos.getZ() + 0.5D;
    }

    /**
     * Tick handler that plays a completion chime once all quick-build
     * placements in a tick batch have finished.
     */
    public static void tickQuickBuildCompletionSound(ServerPlayer player, RtsStorageSession session) {
        if (player == null || session == null || session.quickBuildSoundPlacedCount <= 0) {
            return;
        }
        long gameTime = player.serverLevel().getGameTime();
        if (gameTime < session.quickBuildCompletionSoundTick) {
            return;
        }
        RtsStorageManager.sendDirectSound(
                player,
                SoundEvents.NOTE_BLOCK_HARP.value(),
                SoundSource.PLAYERS,
                session.quickBuildSoundX,
                session.quickBuildSoundY,
                session.quickBuildSoundZ,
                0.35F,
                1.12F);
        session.quickBuildSoundPlacedCount = 0;
        session.quickBuildCompletionSoundTick = -1L;
        session.lastQuickBuildPlaceSoundTick = Long.MIN_VALUE;
    }
}
