package com.rtsbuilding.rtsbuilding.network;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRtsModPresentPayload() implements CustomPacketPayload {
    public static final Type<C2SRtsModPresentPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "c2s_mod_present"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRtsModPresentPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {},
            (buf) -> new C2SRtsModPresentPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}