package com.rtsbuilding.rtsbuilding.network;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRtsLinkedQuickMovePayload(String itemId) implements CustomPacketPayload {
    public static final Type<C2SRtsLinkedQuickMovePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "c2s_rts_linked_quick_move"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRtsLinkedQuickMovePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.itemId(), 128),
            (buf) -> new C2SRtsLinkedQuickMovePayload(buf.readUtf(128)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
