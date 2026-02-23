package com.immersiveautopilot.network;

import com.immersiveautopilot.route.RouteProgram;
import com.immersiveautopilot.screen.RouteOfferScreen;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class S2CRouteOfferToPilot extends Message {
    public static final CustomPacketPayload.Type<S2CRouteOfferToPilot> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "route_offer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRouteOfferToPilot> STREAM_CODEC = StreamCodec.ofMember(S2CRouteOfferToPilot::encode, S2CRouteOfferToPilot::new);

    private final int vehicleId;
    private final String operatorName;
    private final RouteProgram program;

    public S2CRouteOfferToPilot(int vehicleId, String operatorName, RouteProgram program) {
        this.vehicleId = vehicleId;
        this.operatorName = operatorName;
        this.program = program;
    }

    public S2CRouteOfferToPilot(RegistryFriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
        this.operatorName = buf.readUtf();
        this.program = RouteProgram.readFromBuf(buf);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(vehicleId);
        buf.writeUtf(operatorName);
        program.writeToBuf(buf);
    }

    @Override
    public void receiveClient() {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new RouteOfferScreen(vehicleId, operatorName, program)));
    }

    @Override
    public CustomPacketPayload.Type<S2CRouteOfferToPilot> type() {
        return TYPE;
    }
}
