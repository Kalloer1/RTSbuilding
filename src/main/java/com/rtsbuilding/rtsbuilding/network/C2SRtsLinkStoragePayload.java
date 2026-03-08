package com.rtsbuilding.rtsbuilding.network;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRtsLinkStoragePayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<C2SRtsLinkStoragePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "c2s_rts_link_storage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRtsLinkStoragePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBlockPos(payload.pos()),
            (buf) -> new C2SRtsLinkStoragePayload(buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

