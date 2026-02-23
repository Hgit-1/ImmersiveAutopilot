package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import com.immersiveautopilot.route.RouteProgram;
import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class C2SSendRouteToAircraft extends Message {
    public static final CustomPacketPayload.Type<C2SSendRouteToAircraft> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "send_route"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSendRouteToAircraft> STREAM_CODEC = StreamCodec.ofMember(C2SSendRouteToAircraft::encode, C2SSendRouteToAircraft::new);

    private final BlockPos pos;
    private final int entityId;
    private final RouteProgram program;

    public C2SSendRouteToAircraft(BlockPos pos, int entityId, RouteProgram program) {
        this.pos = pos;
        this.entityId = entityId;
        this.program = program;
    }

    public C2SSendRouteToAircraft(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.entityId = buf.readInt();
        this.program = RouteProgram.readFromBuf(buf);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(entityId);
        program.writeToBuf(buf);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        Level level = player.level();
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof TowerBlockEntity tower)) {
            return;
        }
        tower.setActiveRoute(program);
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof VehicleEntity vehicle)) {
            S2CRouteResultToOperator.sendToPlayer(player, RouteResultType.INVALID_TARGET);
            return;
        }
        double dist = vehicle.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        if (dist > tower.getScanRange()) {
            S2CRouteResultToOperator.sendToPlayer(player, RouteResultType.INVALID_TARGET);
            return;
        }
        LivingEntity pilot = vehicle.getControllingPassenger();
        if (!(pilot instanceof ServerPlayer pilotPlayer)) {
            S2CRouteResultToOperator.sendToPlayer(player, RouteResultType.NO_PILOT);
            return;
        }

        java.util.List<com.immersiveautopilot.route.RouteEntry> entries = new java.util.ArrayList<>();
        entries.add(new com.immersiveautopilot.route.RouteEntry(program.getName(), program));
        RouteOfferManager.createOffer(pilotPlayer.getUUID(), player.getUUID(), vehicle.getId(), entries, level.getGameTime());
        immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(new S2CRouteOfferToPilot(vehicle.getId(), player.getName().getString(), entries), pilotPlayer);
        S2CRouteResultToOperator.sendToPlayer(player, RouteResultType.SENT);
    }

    @Override
    public CustomPacketPayload.Type<C2SSendRouteToAircraft> type() {
        return TYPE;
    }
}
