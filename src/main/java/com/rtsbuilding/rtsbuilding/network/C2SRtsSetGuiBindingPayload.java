package com.rtsbuilding.rtsbuilding.network;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRtsSetGuiBindingPayload(
        byte slot,
        boolean clear,
        BlockPos pos) implements CustomPacketPayload {
    public static final Type<C2SRtsSetGuiBindingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "c2s_rts_set_gui_binding"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRtsSetGuiBindingPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeByte(payload.slot());
                buf.writeBoolean(payload.clear());
                buf.writeBlockPos(payload.pos() == null ? BlockPos.ZERO : payload.pos());
            },
            (buf) -> new C2SRtsSetGuiBindingPayload(
                    buf.readByte(),
                    buf.readBoolean(),
                    buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
