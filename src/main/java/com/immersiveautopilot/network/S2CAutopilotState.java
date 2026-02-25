package com.immersiveautopilot.network;

import com.immersiveautopilot.autopilot.AutopilotSupport;
import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class S2CAutopilotState extends Message {
    public static final CustomPacketPayload.Type<S2CAutopilotState> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "autopilot_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CAutopilotState> STREAM_CODEC = StreamCodec.ofMember(S2CAutopilotState::encode, S2CAutopilotState::new);

    private final int vehicleId;
    private final boolean enabled;

    public S2CAutopilotState(int vehicleId, boolean enabled) {
        this.vehicleId = vehicleId;
        this.enabled = enabled;
    }

    public S2CAutopilotState(RegistryFriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
        this.enabled = buf.readBoolean();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(vehicleId);
        buf.writeBoolean(enabled);
    }

    @Override
    public void receiveClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (mc.level.getEntity(vehicleId) instanceof VehicleEntity vehicle) {
            AutopilotSupport.setAutopilotEnabled(vehicle, enabled);
        }
    }

    @Override
    public CustomPacketPayload.Type<S2CAutopilotState> type() {
        return TYPE;
    }
}
