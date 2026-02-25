package com.immersiveautopilot.client;

import com.immersiveautopilot.autopilot.AutopilotSupport;
import com.immersiveautopilot.network.C2SToggleAutopilot;
import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class AutopilotToggleHandler {
    private AutopilotToggleHandler() {
    }

    public static void toggleAutopilot() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (!AutopilotSupport.hasAutopilot(vehicle)) {
            return;
        }
        if (!AutopilotSupport.isAutopilotSupported(vehicle)) {
            AutopilotStatusOverlay.showUnsupported();
            return;
        }
        boolean next = !AutopilotSupport.isAutopilotEnabled(vehicle);
        AutopilotSupport.setAutopilotEnabled(vehicle, next);
        NetworkHandler.sendToServer(new C2SToggleAutopilot(vehicle.getId(), next));
    }
}
