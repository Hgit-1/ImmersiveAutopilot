package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class C2SDeletePreset extends Message {
    public static final CustomPacketPayload.Type<C2SDeletePreset> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "delete_preset"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SDeletePreset> STREAM_CODEC = StreamCodec.ofMember(C2SDeletePreset::encode, C2SDeletePreset::new);

    private final BlockPos pos;
    private final String name;

    public C2SDeletePreset(BlockPos pos, String name) {
        this.pos = pos;
        this.name = name;
    }

    public C2SDeletePreset(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.name = buf.readUtf();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(name);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            tower.deletePreset(name);
            S2CTowerState.sendToPlayer(player, tower);
        }
    }

    @Override
    public CustomPacketPayload.Type<C2SDeletePreset> type() {
        return TYPE;
    }
}
