package com.rtsbuilding.rtsbuilding.network.builder;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端→服务端：删除/取消指定索引的工作流。
 *
 * @param workflowIndex 要删除的工作流在客户端数组中的索引（0-7）
 */
public record C2SRtsDeleteWorkflowPayload(int workflowIndex) implements CustomPacketPayload {
    public static final Type<C2SRtsDeleteWorkflowPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RtsbuildingMod.MODID, "c2s_rts_delete_workflow"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRtsDeleteWorkflowPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            C2SRtsDeleteWorkflowPayload::workflowIndex,
            C2SRtsDeleteWorkflowPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
