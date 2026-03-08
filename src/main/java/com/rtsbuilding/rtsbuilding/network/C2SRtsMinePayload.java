package com.rtsbuilding.rtsbuilding.network;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRtsMinePayload(
        BlockPos pos,
        byte face,
        boolean start,
        byte toolSlot) implements CustomPacketPayload {
    public static final Type<C2SRtsMinePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "c2s_rts_mine"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRtsMinePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeByte(payload.face());
                buf.writeBoolean(payload.start());
                buf.writeByte(payload.toolSlot());
            },
            (buf) -> new C2SRtsMinePayload(
                    buf.readBlockPos(),
                    buf.readByte(),
                    buf.readBoolean(),
                    buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
