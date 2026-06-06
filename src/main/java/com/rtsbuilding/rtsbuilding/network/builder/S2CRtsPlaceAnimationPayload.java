package com.rtsbuilding.rtsbuilding.network.builder;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server confirmation that an RTS block placement actually succeeded.
 *
 * <p>The client treats this as a purely visual cue. It must not drive gameplay
 * state, inventory counts, undo history, or placement retries; those stay
 * authoritative on the server-side placement path.
 */
public record S2CRtsPlaceAnimationPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<S2CRtsPlaceAnimationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "s2c_rts_place_animation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRtsPlaceAnimationPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBlockPos(payload.pos()),
            (buf) -> new S2CRtsPlaceAnimationPayload(buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
