package com.immersiveautopilot.network;

import com.immersiveautopilot.route.AutopilotRouteHolder;
import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class C2SPilotRouteDecision extends Message {
    public static final CustomPacketPayload.Type<C2SPilotRouteDecision> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "route_decision"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SPilotRouteDecision> STREAM_CODEC = StreamCodec.ofMember(C2SPilotRouteDecision::encode, C2SPilotRouteDecision::new);

    private final int vehicleId;
    private final boolean accept;

    public C2SPilotRouteDecision(int vehicleId, boolean accept) {
        this.vehicleId = vehicleId;
        this.accept = accept;
    }

    public C2SPilotRouteDecision(RegistryFriendlyByteBuf buf) {
        this.vehicleId = buf.readInt();
        this.accept = buf.readBoolean();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(vehicleId);
        buf.writeBoolean(accept);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        PendingRouteOffer offer = RouteOfferManager.getOffer(player.getUUID());
        if (offer == null) {
            S2CRouteResultToOperator.sendToPlayer(player, RouteResultType.EXPIRED);
            return;
        }
        if (offer.vehicleId() != vehicleId) {
            RouteOfferManager.consumeOffer(player.getUUID());
            S2CRouteResultToOperator.sendToOperator(offer.operatorUuid(), RouteResultType.INVALID_TARGET);
            return;
        }
        if (RouteOfferManager.isExpired(offer, player.level().getGameTime())) {
            RouteOfferManager.consumeOffer(player.getUUID());
            S2CRouteResultToOperator.sendToOperator(offer.operatorUuid(), RouteResultType.EXPIRED);
            return;
        }
        Entity entity = player.level().getEntity(vehicleId);
        if (!(entity instanceof VehicleEntity vehicle) || vehicle.getControllingPassenger() != player) {
            RouteOfferManager.consumeOffer(player.getUUID());
            S2CRouteResultToOperator.sendToOperator(offer.operatorUuid(), RouteResultType.NOT_PILOT);
            return;
        }

        RouteOfferManager.consumeOffer(player.getUUID());
        if (accept) {
            if (vehicle instanceof AutopilotRouteHolder holder) {
                holder.setAutopilotRoute(offer.program());
            }
            S2CRouteResultToOperator.sendToOperator(offer.operatorUuid(), RouteResultType.ACCEPTED);
        } else {
            S2CRouteResultToOperator.sendToOperator(offer.operatorUuid(), RouteResultType.DECLINED);
        }
    }

    @Override
    public CustomPacketPayload.Type<C2SPilotRouteDecision> type() {
        return TYPE;
    }
}
