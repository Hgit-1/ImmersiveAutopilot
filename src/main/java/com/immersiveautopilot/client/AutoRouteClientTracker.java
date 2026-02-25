package com.immersiveautopilot.client;

import com.immersiveautopilot.ImmersiveAutopilot;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AutoRouteClientTracker {
    private static int lastVehicleId = -1;

    private AutoRouteClientTracker() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            lastVehicleId = -1;
            return;
        }
        int vehicleId = -1;
        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            vehicleId = vehicle.getId();
        }
        if (vehicleId != -1 && vehicleId != lastVehicleId) {
            AutoRouteClient.requestRoutes(vehicleId);
        }
        if (vehicleId == -1 && lastVehicleId != -1) {
            ClientRouteGuidance.onAirspaceExit(lastVehicleId);
            XaeroBridge.clearTemporaryWaypoints();
        }
        lastVehicleId = vehicleId;
    }
}
