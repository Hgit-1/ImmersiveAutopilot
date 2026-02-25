package com.immersiveautopilot.network;

import com.immersiveautopilot.client.ClientRouteGuidance;
import com.immersiveautopilot.client.AutoRouteClient;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class S2CAirspaceState extends Message {
    public static final CustomPacketPayload.Type<S2CAirspaceState> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "airspace_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CAirspaceState> STREAM_CODEC = StreamCodec.ofMember(S2CAirspaceState::encode, S2CAirspaceState::new);

    private final int vehicleId;
    private final boolean inAirspace;

    public S2CAirspaceState(int vehicleId, boolean inAirspace) {
        this.vehicleId = vehicleId;
        this.inAirspace = inAirspace;
    }

    public S2CAirspaceState(RegistryFriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
        this.inAirspace = buf.readBoolean();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(vehicleId);
        buf.writeBoolean(inAirspace);
    }

    @Override
    public void receiveClient() {
        Minecraft.getInstance().execute(() -> {
            if (inAirspace) {
                ClientRouteGuidance.onAirspaceEnter(vehicleId);
                AutoRouteClient.onAirspaceEnter(vehicleId);
            } else {
                ClientRouteGuidance.onAirspaceExit(vehicleId);
                AutoRouteClient.onAirspaceExit(vehicleId);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<S2CAirspaceState> type() {
        return TYPE;
    }
}
