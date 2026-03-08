package com.rtsbuilding.rtsbuilding.network;

import java.util.ArrayList;
import java.util.List;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CRtsStoragePagePayload(
        boolean linked,
        String linkedName,
        List<Long> linkedPositions,
        int page,
        int totalPages,
        int totalEntries,
        String search,
        String category,
        byte sort,
        boolean ascending,
        boolean autoStoreMinedDrops,
        List<String> categories,
        List<String> itemIds,
        List<Long> counts,
        List<String> fluidIds,
        List<Long> fluidAmounts,
        List<Long> fluidCapacities,
        boolean funnelEnabled,
        List<String> funnelBufferItemIds,
        List<Long> funnelBufferCounts) implements CustomPacketPayload {
    public static final Type<S2CRtsStoragePagePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "s2c_rts_storage_page"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRtsStoragePagePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.linked());
                buf.writeUtf(payload.linkedName(), 128);
                buf.writeVarInt(payload.linkedPositions().size());
                for (Long packedPos : payload.linkedPositions()) {
                    buf.writeLong(packedPos == null ? 0L : packedPos.longValue());
                }
                buf.writeVarInt(payload.page());
                buf.writeVarInt(payload.totalPages());
                buf.writeVarInt(payload.totalEntries());
                buf.writeUtf(payload.search(), 128);
                buf.writeUtf(payload.category(), 128);
                buf.writeByte(payload.sort());
                buf.writeBoolean(payload.ascending());
                buf.writeBoolean(payload.autoStoreMinedDrops());

                buf.writeVarInt(payload.categories().size());
                for (String category : payload.categories()) {
                    buf.writeUtf(category, 128);
                }

                int size = Math.min(payload.itemIds().size(), payload.counts().size());
                buf.writeVarInt(size);
                for (int i = 0; i < size; i++) {
                    buf.writeUtf(payload.itemIds().get(i), 128);
                    buf.writeVarLong(payload.counts().get(i));
                }

                int fluidSize = Math.min(payload.fluidIds().size(),
                        Math.min(payload.fluidAmounts().size(), payload.fluidCapacities().size()));
                buf.writeVarInt(fluidSize);
                for (int i = 0; i < fluidSize; i++) {
                    buf.writeUtf(payload.fluidIds().get(i), 128);
                    buf.writeVarLong(payload.fluidAmounts().get(i));
                    buf.writeVarLong(payload.fluidCapacities().get(i));
                }

                buf.writeBoolean(payload.funnelEnabled());
                int funnelBufferSize = Math.min(payload.funnelBufferItemIds().size(), payload.funnelBufferCounts().size());
                buf.writeVarInt(funnelBufferSize);
                for (int i = 0; i < funnelBufferSize; i++) {
                    buf.writeUtf(payload.funnelBufferItemIds().get(i), 128);
                    buf.writeVarLong(payload.funnelBufferCounts().get(i));
                }
            },
            (buf) -> {
                boolean linked = buf.readBoolean();
                String linkedName = buf.readUtf(128);
                int linkedPosSize = buf.readVarInt();
                List<Long> linkedPositions = new ArrayList<>(linkedPosSize);
                for (int i = 0; i < linkedPosSize; i++) {
                    linkedPositions.add(buf.readLong());
                }
                int page = buf.readVarInt();
                int totalPages = buf.readVarInt();
                int totalEntries = buf.readVarInt();
                String search = buf.readUtf(128);
                String category = buf.readUtf(128);
                byte sort = buf.readByte();
                boolean ascending = buf.readBoolean();
                boolean autoStoreMinedDrops = buf.readBoolean();
                int categorySize = buf.readVarInt();
                List<String> categories = new ArrayList<>(categorySize);
                for (int i = 0; i < categorySize; i++) {
                    categories.add(buf.readUtf(128));
                }
                int size = buf.readVarInt();
                List<String> itemIds = new ArrayList<>(size);
                List<Long> counts = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    itemIds.add(buf.readUtf(128));
                    counts.add(buf.readVarLong());
                }
                int fluidSize = buf.readVarInt();
                List<String> fluidIds = new ArrayList<>(fluidSize);
                List<Long> fluidAmounts = new ArrayList<>(fluidSize);
                List<Long> fluidCapacities = new ArrayList<>(fluidSize);
                for (int i = 0; i < fluidSize; i++) {
                    fluidIds.add(buf.readUtf(128));
                    fluidAmounts.add(buf.readVarLong());
                    fluidCapacities.add(buf.readVarLong());
                }
                boolean funnelEnabled = buf.readBoolean();
                int funnelBufferSize = buf.readVarInt();
                List<String> funnelBufferItemIds = new ArrayList<>(funnelBufferSize);
                List<Long> funnelBufferCounts = new ArrayList<>(funnelBufferSize);
                for (int i = 0; i < funnelBufferSize; i++) {
                    funnelBufferItemIds.add(buf.readUtf(128));
                    funnelBufferCounts.add(buf.readVarLong());
                }
                return new S2CRtsStoragePagePayload(
                        linked,
                        linkedName,
                        linkedPositions,
                        page,
                        totalPages,
                        totalEntries,
                        search,
                        category,
                        sort,
                        ascending,
                        autoStoreMinedDrops,
                        categories,
                        itemIds,
                        counts,
                        fluidIds,
                        fluidAmounts,
                        fluidCapacities,
                        funnelEnabled,
                        funnelBufferItemIds,
                        funnelBufferCounts);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
