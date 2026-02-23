package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class C2SUnbindAircraft extends Message {
    public static final CustomPacketPayload.Type<C2SUnbindAircraft> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "unbind_aircraft"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SUnbindAircraft> STREAM_CODEC = StreamCodec.ofMember(C2SUnbindAircraft::encode, C2SUnbindAircraft::new);

    private final BlockPos pos;

    public C2SUnbindAircraft(BlockPos pos) {
        this.pos = pos;
    }

    public C2SUnbindAircraft(RegistryFriendlyByteBuf buf) {
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
        if (player.level().getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            tower.setBoundAircraft(null);
            S2CTowerState.sendToPlayer(player, tower);
        }
    }

    @Override
    public CustomPacketPayload.Type<C2SUnbindAircraft> type() {
        return TYPE;
    }
}
