package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.RadarBlockEntity;
import com.immersiveautopilot.blockentity.TowerBlockEntity;
import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.core.BlockPos;
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
import java.util.UUID;

public class C2SRequestRadarAircraftList extends Message {
    public static final CustomPacketPayload.Type<C2SRequestRadarAircraftList> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "request_radar_aircraft_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestRadarAircraftList> STREAM_CODEC = StreamCodec.ofMember(C2SRequestRadarAircraftList::encode, C2SRequestRadarAircraftList::new);

    private final BlockPos pos;

    public C2SRequestRadarAircraftList(BlockPos pos) {
        this.pos = pos;
    }

    public C2SRequestRadarAircraftList(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        Level level = player.level();
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof RadarBlockEntity radar)) {
            return;
        }
        int range = TowerBlockEntity.DEFAULT_SCAN_RANGE + radar.getRangeBonus();
        boolean showUnbound = radar.hasIdentModule();
        UUID boundUuid = findBoundAircraft(level, pos);

        Vec3 center = Vec3.atCenterOf(pos);
        AABB box = new AABB(pos).inflate(range);
        List<com.immersiveautopilot.data.AircraftSnapshot> snapshots = new ArrayList<>();
        for (VehicleEntity vehicle : level.getEntitiesOfClass(VehicleEntity.class, box)) {
            if (!showUnbound && boundUuid != null && !boundUuid.equals(vehicle.getUUID())) {
                continue;
            }
            snapshots.add(com.immersiveautopilot.network.S2CAircraftList.buildSnapshot(vehicle, center));
        }
        com.immersiveautopilot.network.S2CAircraftList.sendToPlayer(player, pos, snapshots);
    }

    private UUID findBoundAircraft(Level level, BlockPos radarPos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    mutable.set(radarPos.getX() + dx, radarPos.getY() + dy, radarPos.getZ() + dz);
                    if (level.getBlockEntity(mutable) instanceof TowerBlockEntity tower) {
                        return tower.getBoundAircraft();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public CustomPacketPayload.Type<C2SRequestRadarAircraftList> type() {
        return TYPE;
    }
}
