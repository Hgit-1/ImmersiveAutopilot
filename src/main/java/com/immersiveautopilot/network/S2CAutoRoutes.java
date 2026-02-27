package com.immersiveautopilot.network;

import com.immersiveautopilot.data.AutoRouteSavedData;
import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class S2CAutoRoutes extends Message {
    public static final CustomPacketPayload.Type<S2CAutoRoutes> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "auto_routes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CAutoRoutes> STREAM_CODEC = StreamCodec.ofMember(S2CAutoRoutes::encode, S2CAutoRoutes::new);

    private final int vehicleId;
    private final List<String> names;
    private final List<String> labels;

    public S2CAutoRoutes(int vehicleId, List<String> names, List<String> labels) {
        this.vehicleId = vehicleId;
        this.names = names == null ? List.of() : new ArrayList<>(names);
        this.labels = labels == null ? List.of() : new ArrayList<>(labels);
    }

    public S2CAutoRoutes(RegistryFriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
        int count = buf.readInt();
        this.names = new ArrayList<>(count);
        this.labels = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            names.add(buf.readUtf());
            labels.add(buf.readUtf());
        }
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(vehicleId);
        buf.writeInt(names.size());
        for (int i = 0; i < names.size(); i++) {
            buf.writeUtf(names.get(i));
            buf.writeUtf(i < labels.size() ? labels.get(i) : "");
        }
    }

    @Override
    public void receiveClient() {
        ClientSideExecutor.run("handleAutoRoutes", S2CAutoRoutes.class, this);
    }

    @Override
    public CustomPacketPayload.Type<S2CAutoRoutes> type() {
        return TYPE;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public List<String> getNames() {
        return names;
    }

    public List<String> getLabels() {
        return labels;
    }

    public static void sendToPlayer(ServerPlayer player, VehicleEntity vehicle, List<AutoRouteSavedData.Entry> entries) {
        List<String> names = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (AutoRouteSavedData.Entry entry : entries) {
            names.add(entry.name());
            labels.add(entry.label());
        }
        immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(new S2CAutoRoutes(vehicle.getId(), names, labels), player);
    }
}
