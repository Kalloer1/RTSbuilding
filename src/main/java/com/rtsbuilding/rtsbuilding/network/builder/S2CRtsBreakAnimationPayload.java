package com.rtsbuilding.rtsbuilding.network.builder;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server confirmation that an RTS block break actually succeeded.
 *
 * <p>The client treats this as a purely visual cue. It must not drive gameplay
 * state, tool durability, drops, or retry behaviour; those remain authoritative
 * on the server-side mining path.
 */
public record S2CRtsBreakAnimationPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<S2CRtsBreakAnimationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "s2c_rts_break_animation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRtsBreakAnimationPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBlockPos(payload.pos()),
            (buf) -> new S2CRtsBreakAnimationPayload(buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
