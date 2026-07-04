package com.rtsbuilding.rtsbuilding.network.storage;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRtsSwapHotbarSlotPayload(byte slot, String targetItemId) implements CustomPacketPayload {
    public static final Type<C2SRtsSwapHotbarSlotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "c2s_rts_swap_hotbar_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRtsSwapHotbarSlotPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeByte(payload.slot());
                buf.writeUtf(payload.targetItemId());
            },
            (buf) -> new C2SRtsSwapHotbarSlotPayload(buf.readByte(), buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
