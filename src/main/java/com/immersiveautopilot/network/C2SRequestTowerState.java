package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class C2SRequestTowerState extends Message {
    public static final CustomPacketPayload.Type<C2SRequestTowerState> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "request_tower_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestTowerState> STREAM_CODEC = StreamCodec.ofMember(C2SRequestTowerState::encode, C2SRequestTowerState::new);

    private final BlockPos pos;

    public C2SRequestTowerState(BlockPos pos) {
        this.pos = pos;
    }

    public C2SRequestTowerState(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        Level level = player.level();
        if (level.getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            S2CTowerState.sendToPlayer(player, tower);
        }
    }

    @Override
    public CustomPacketPayload.Type<C2SRequestTowerState> type() {
        return TYPE;
    }
}
