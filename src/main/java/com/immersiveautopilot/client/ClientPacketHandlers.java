package com.immersiveautopilot.client;

import com.immersiveautopilot.autopilot.AutopilotSupport;
import com.immersiveautopilot.network.S2CAirspaceState;
import com.immersiveautopilot.network.S2CAutopilotState;
import com.immersiveautopilot.network.S2CAutoRoutes;
import com.immersiveautopilot.network.S2CRouteOfferToPilot;
import com.immersiveautopilot.network.S2CRouteResultToOperator;
import com.immersiveautopilot.route.RouteEntry;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void handleAutopilotState(S2CAutopilotState packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (mc.level.getEntity(packet.getVehicleId()) instanceof VehicleEntity vehicle) {
            AutopilotSupport.setAutopilotEnabled(vehicle, packet.isEnabled());
        }
    }

    public static void handleAirspaceState(S2CAirspaceState packet) {
        Minecraft.getInstance().execute(() -> {
            if (packet.isInAirspace()) {
                ClientRouteGuidance.onAirspaceEnter(packet.getVehicleId());
                AutoRouteClient.onAirspaceEnter(packet.getVehicleId());
            } else {
                ClientRouteGuidance.onAirspaceExit(packet.getVehicleId());
                AutoRouteClient.onAirspaceExit(packet.getVehicleId());
            }
        });
    }

    public static void handleAutoRoutes(S2CAutoRoutes packet) {
        Minecraft.getInstance().execute(() -> {
            List<AutoRouteClient.Entry> entries = new ArrayList<>();
            List<String> names = packet.getNames();
            List<String> labels = packet.getLabels();
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                String label = i < labels.size() ? labels.get(i) : "";
                entries.add(new AutoRouteClient.Entry(name, label));
            }
            AutoRouteClient.setRoutes(packet.getVehicleId(), entries);
        });
    }

    public static void handleRouteOffer(S2CRouteOfferToPilot packet) {
        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            if (mc.player.getVehicle() == null || mc.player.getVehicle().getId() != packet.getVehicleId()) {
                return;
            }
            if (ClientRouteGuidance.shouldSuppressOffers(packet.getVehicleId())) {
                return;
            }
            List<RouteEntry> entries = packet.getEntries();
            AutoRouteClient.registerOffer(packet.getVehicleId(), packet.getOperatorName(), entries);
            if (AutoRouteClient.isAccepted(packet.getVehicleId()) || !AutoRouteClient.getPending(packet.getVehicleId()).isEmpty()) {
                return;
            }
            mc.setScreen(new com.immersiveautopilot.screen.RouteOfferScreen(packet.getVehicleId(), packet.getOperatorName(), entries));
        });
    }

    public static void handleRouteResult(S2CRouteResultToOperator packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Component message = switch (packet.getResult()) {
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
}
