package com.immersiveautopilot.network;

import com.immersiveautopilot.client.ClientCache;
import com.immersiveautopilot.data.AircraftSnapshot;
import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class S2CAircraftList extends Message {
    public static final CustomPacketPayload.Type<S2CAircraftList> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "aircraft_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CAircraftList> STREAM_CODEC = StreamCodec.ofMember(S2CAircraftList::encode, S2CAircraftList::new);

    private final BlockPos pos;
    private final List<AircraftSnapshot> list;

    public S2CAircraftList(BlockPos pos, List<AircraftSnapshot> list) {
        this.pos = pos;
        this.list = list;
    }

    public S2CAircraftList(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        int count = buf.readInt();
        this.list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(AircraftSnapshot.readFromBuf(buf));
        }
    }

    public static void sendToPlayer(ServerPlayer player, BlockPos towerPos, int range) {
        Level level = player.level();
        Vec3 center = Vec3.atCenterOf(towerPos);
        AABB box = new AABB(towerPos).inflate(range);

        List<AircraftSnapshot> snapshots = new ArrayList<>();
        for (VehicleEntity vehicle : level.getEntitiesOfClass(VehicleEntity.class, box)) {
            snapshots.add(buildSnapshot(vehicle, center));
        }

        immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(new S2CAircraftList(towerPos, snapshots), player);
    }

    public static void sendToPlayer(ServerPlayer player, BlockPos pos, List<AircraftSnapshot> snapshots) {
        immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(new S2CAircraftList(pos, snapshots), player);
    }

    public static AircraftSnapshot buildSnapshot(VehicleEntity vehicle, Vec3 center) {
        double distance = vehicle.position().distanceTo(center);
        double altitude = vehicle.getY();
        Vec3 velocity = vehicle.getDeltaMovement();
        double speed = velocity.length();
        float enginePower = 0.0f;
        float fuel = 0.0f;
        if (vehicle instanceof EngineVehicle engineVehicle) {
            enginePower = engineVehicle.getEnginePower();
            fuel = engineVehicle.getFuelUtilization();
        }
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        String name = vehicle.getDisplayName().getString();
        return new AircraftSnapshot(vehicle.getId(), vehicle.getUUID(), name, typeId, distance, altitude, speed, enginePower, fuel,
                vehicle.getHealth(), vehicle.getX(), vehicle.getZ(), velocity.x, velocity.y, velocity.z);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(list.size());
        for (AircraftSnapshot snapshot : list) {
            snapshot.writeToBuf(buf);
        }
    }

    @Override
    public void receiveClient() {
        ClientCache.setAircraftList(pos, list);
    }

    @Override
    public CustomPacketPayload.Type<S2CAircraftList> type() {
        return TYPE;
    }
}
