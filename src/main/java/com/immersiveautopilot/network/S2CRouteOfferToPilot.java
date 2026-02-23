package com.immersiveautopilot.network;

import com.immersiveautopilot.route.RouteEntry;
import com.immersiveautopilot.route.RouteProgram;
import com.immersiveautopilot.screen.RouteOfferScreen;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class S2CRouteOfferToPilot extends Message {
    public static final CustomPacketPayload.Type<S2CRouteOfferToPilot> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "route_offer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRouteOfferToPilot> STREAM_CODEC = StreamCodec.ofMember(S2CRouteOfferToPilot::encode, S2CRouteOfferToPilot::new);

    private final int vehicleId;
    private final String operatorName;
    private final List<RouteEntry> entries;

    public S2CRouteOfferToPilot(int vehicleId, String operatorName, List<RouteEntry> entries) {
        this.vehicleId = vehicleId;
        this.operatorName = operatorName;
        this.entries = entries;
    }

    public S2CRouteOfferToPilot(RegistryFriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
        this.operatorName = buf.readUtf();
        int count = buf.readInt();
        this.entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf();
            RouteProgram program = RouteProgram.readFromBuf(buf);
            entries.add(new RouteEntry(name, program));
        }
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(vehicleId);
        buf.writeUtf(operatorName);
        buf.writeInt(entries.size());
        for (RouteEntry entry : entries) {
            buf.writeUtf(entry.name());
            entry.program().writeToBuf(buf);
        }
    }

    @Override
    public void receiveClient() {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player == null) {
                return;
            }
            if (Minecraft.getInstance().player.getVehicle() == null || Minecraft.getInstance().player.getVehicle().getId() != vehicleId) {
                return;
            }
            Minecraft.getInstance().setScreen(new RouteOfferScreen(vehicleId, operatorName, entries));
        });
    }

    @Override
    public CustomPacketPayload.Type<S2CRouteOfferToPilot> type() {
        return TYPE;
    }
}
