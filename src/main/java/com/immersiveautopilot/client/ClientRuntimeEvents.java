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
public final class ClientRuntimeEvents {
    private ClientRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientRouteGuidance.tick();
        while (AutopilotKeyBindings.TOGGLE_AUTOPILOT.consumeClick()) {
            AutopilotToggleHandler.toggleAutopilot();
        }
        while (AutopilotKeyBindings.CYCLE_ROUTE.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) {
                continue;
            }
            if (player.getVehicle() instanceof VehicleEntity vehicle) {
                AutoRouteClient.cycleRoute(vehicle.getId());
            }
        }
    }
}
