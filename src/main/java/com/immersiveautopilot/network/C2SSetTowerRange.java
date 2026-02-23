package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class C2SSetTowerRange extends Message {
    public static final CustomPacketPayload.Type<C2SSetTowerRange> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "set_tower_range"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetTowerRange> STREAM_CODEC = StreamCodec.ofMember(C2SSetTowerRange::encode, C2SSetTowerRange::new);

    private final BlockPos pos;
    private final int range;

    public C2SSetTowerRange(BlockPos pos, int range) {
        this.pos = pos;
        this.range = range;
    }

    public C2SSetTowerRange(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.range = buf.readInt();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(range);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            tower.setScanRange(range);
            S2CTowerState.sendToPlayer(player, tower);
        }
    }

    @Override
    public CustomPacketPayload.Type<C2SSetTowerRange> type() {
        return TYPE;
    }
}
