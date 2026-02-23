package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class C2SBindAircraft extends Message {
    public static final CustomPacketPayload.Type<C2SBindAircraft> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "bind_aircraft"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SBindAircraft> STREAM_CODEC = StreamCodec.ofMember(C2SBindAircraft::encode, C2SBindAircraft::new);

    private final BlockPos pos;
    private final int entityId;

    public C2SBindAircraft(BlockPos pos, int entityId) {
        this.pos = pos;
        this.entityId = entityId;
    }

    public C2SBindAircraft(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.entityId = buf.readInt();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(entityId);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof VehicleEntity vehicle) {
                tower.setBoundAircraft(vehicle.getUUID());
                S2CTowerState.sendToPlayer(player, tower);
            }
        }
    }

    @Override
    public CustomPacketPayload.Type<C2SBindAircraft> type() {
        return TYPE;
    }
}
