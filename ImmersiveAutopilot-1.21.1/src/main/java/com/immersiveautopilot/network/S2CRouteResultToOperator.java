package com.immersiveautopilot.network;

import immersive_aircraft.cobalt.network.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

public class S2CRouteResultToOperator extends Message {
    public static final CustomPacketPayload.Type<S2CRouteResultToOperator> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "route_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRouteResultToOperator> STREAM_CODEC = StreamCodec.ofMember(S2CRouteResultToOperator::encode, S2CRouteResultToOperator::new);

    private final RouteResultType result;

    public S2CRouteResultToOperator(RouteResultType result) {
        this.result = result;
    }

    public S2CRouteResultToOperator(RegistryFriendlyByteBuf buf) {
        this.result = RouteResultType.values()[buf.readInt()];
    }

    public static void sendToPlayer(ServerPlayer player, RouteResultType result) {
        immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(new S2CRouteResultToOperator(result), player);
    }

    public static void sendToOperator(UUID operatorUuid, RouteResultType result) {
        if (operatorUuid == null) {
            return;
        }
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(operatorUuid);
        if (player != null) {
            sendToPlayer(player, result);
        }
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(result.ordinal());
    }

    @Override
    public void receiveClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Component message = switch (result) {
            case SENT -> Component.translatable("message.immersive_autopilot.sent");
            case ACCEPTED -> Component.translatable("message.immersive_autopilot.accepted");
            case DECLINED -> Component.translatable("message.immersive_autopilot.declined");
            case NO_PILOT -> Component.translatable("message.immersive_autopilot.no_pilot");
            case INVALID_TARGET -> Component.translatable("message.immersive_autopilot.invalid_target");
            case EXPIRED -> Component.translatable("message.immersive_autopilot.expired");
            case NOT_PILOT -> Component.translatable("message.immersive_autopilot.not_pilot");
        };
        mc.player.displayClientMessage(message, true);
    }

    @Override
    public CustomPacketPayload.Type<S2CRouteResultToOperator> type() {
        return TYPE;
    }
}
