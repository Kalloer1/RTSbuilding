package com.rtsbuilding.rtsbuilding.network.builder;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRtsAttackEntityPayload(
        int entityId,
        int toolSlot) implements CustomPacketPayload {

    public static final Type<C2SRtsAttackEntityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "c2s_rts_attack_entity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRtsAttackEntityPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.entityId());
                buf.writeByte(payload.toolSlot());
            },
            (buf) -> new C2SRtsAttackEntityPayload(
                    buf.readInt(),
                    buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}