package com.immersiveautopilot.network;

import com.immersiveautopilot.data.AutoRouteSavedData;
import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class C2SRequestAutoRoutes extends Message {
    public static final CustomPacketPayload.Type<C2SRequestAutoRoutes> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "request_auto_routes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestAutoRoutes> STREAM_CODEC = StreamCodec.ofMember(C2SRequestAutoRoutes::encode, C2SRequestAutoRoutes::new);

    private final int vehicleId;

    public C2SRequestAutoRoutes(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public C2SRequestAutoRoutes(RegistryFriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(vehicleId);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        if (player.level().getEntity(vehicleId) instanceof VehicleEntity vehicle) {
            var data = AutoRouteSavedData.get(player.serverLevel());
            S2CAutoRoutes.sendToPlayer(player, vehicle, data.getRoutes(vehicle.getUUID()));
        }
    }

    @Override
    public CustomPacketPayload.Type<C2SRequestAutoRoutes> type() {
        return TYPE;
    }
}
