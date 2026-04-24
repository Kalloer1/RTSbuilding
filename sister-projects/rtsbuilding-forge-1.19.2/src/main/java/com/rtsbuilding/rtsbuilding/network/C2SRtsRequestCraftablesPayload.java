package com.rtsbuilding.rtsbuilding.network;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

import com.rtsbuilding.rtsbuilding.forgecompat.network.RegistryFriendlyByteBuf;
import com.rtsbuilding.rtsbuilding.forgecompat.network.StreamCodec;
import com.rtsbuilding.rtsbuilding.forgecompat.network.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRtsRequestCraftablesPayload(
        String search,
        boolean showUnavailable,
        int offset,
        int limit) implements CustomPacketPayload {
    public static final Type<C2SRtsRequestCraftablesPayload> TYPE = new Type<>(
            new ResourceLocation(RtsbuildingMod.MODID, "c2s_rts_request_craftables"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRtsRequestCraftablesPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.search() == null ? "" : payload.search(), 128);
                        buf.writeBoolean(payload.showUnavailable());
                        buf.writeVarInt(Math.max(0, payload.offset()));
                        buf.writeVarInt(Math.max(1, payload.limit()));
                    },
                    (buf) -> new C2SRtsRequestCraftablesPayload(
                            buf.readUtf(128),
                            buf.readBoolean(),
                            buf.readVarInt(),
                            buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}


