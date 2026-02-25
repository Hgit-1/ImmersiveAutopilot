package com.immersiveautopilot.network;

import com.immersiveautopilot.autopilot.AutopilotSupport;
import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class C2SToggleAutopilot extends Message {
    public static final CustomPacketPayload.Type<C2SToggleAutopilot> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "toggle_autopilot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SToggleAutopilot> STREAM_CODEC = StreamCodec.ofMember(C2SToggleAutopilot::encode, C2SToggleAutopilot::new);

    private final int vehicleId;
    private final boolean enabled;

    public C2SToggleAutopilot(int vehicleId, boolean enabled) {
        this.vehicleId = vehicleId;
        this.enabled = enabled;
    }

    public C2SToggleAutopilot(RegistryFriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
        this.enabled = buf.readBoolean();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(vehicleId);
        buf.writeBoolean(enabled);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        if (!(player.level().getEntity(vehicleId) instanceof VehicleEntity vehicle)) {
            return;
        }
        if (vehicle.getControllingPassenger() != player) {
            return;
        }
        if (!AutopilotSupport.hasAutopilot(vehicle)) {
            AutopilotSupport.setAutopilotEnabled(vehicle, false);
            return;
        }
        AutopilotSupport.setAutopilotEnabled(vehicle, enabled);
        immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(
                new S2CAutopilotState(vehicleId, AutopilotSupport.isAutopilotEnabled(vehicle)), player);
    }

    @Override
    public CustomPacketPayload.Type<C2SToggleAutopilot> type() {
        return TYPE;
    }
}
