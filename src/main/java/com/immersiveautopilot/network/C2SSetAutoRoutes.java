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

public class C2SSetAutoRoutes extends Message {
    public static final CustomPacketPayload.Type<C2SSetAutoRoutes> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "set_auto_routes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetAutoRoutes> STREAM_CODEC = StreamCodec.ofMember(C2SSetAutoRoutes::encode, C2SSetAutoRoutes::new);

    private final int vehicleId;
    private final List<String> names;
    private final List<String> labels;

    public C2SSetAutoRoutes(int vehicleId, List<String> names, List<String> labels) {
        this.vehicleId = vehicleId;
        this.names = names == null ? List.of() : new ArrayList<>(names);
        this.labels = labels == null ? List.of() : new ArrayList<>(labels);
    }

    public C2SSetAutoRoutes(RegistryFriendlyByteBuf buf) {
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
    public void receiveServer(ServerPlayer player) {
        if (!(player.level().getEntity(vehicleId) instanceof VehicleEntity vehicle)) {
            return;
        }
        if (!vehicle.hasPassenger(player)) {
            return;
        }
        var data = AutoRouteSavedData.get(player.serverLevel());
        List<AutoRouteSavedData.Entry> entries = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (name == null || name.isBlank()) {
                continue;
            }
            String label = i < labels.size() ? labels.get(i) : "";
            entries.add(new AutoRouteSavedData.Entry(name, label == null ? "" : label));
        }
        data.setRoutes(vehicle.getUUID(), entries);
        S2CAutoRoutes.sendToPlayer(player, vehicle, entries);
    }

    @Override
    public CustomPacketPayload.Type<C2SSetAutoRoutes> type() {
        return TYPE;
    }
}
