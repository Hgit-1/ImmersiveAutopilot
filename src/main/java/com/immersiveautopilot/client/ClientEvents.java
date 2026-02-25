package com.immersiveautopilot.client;

import com.immersiveautopilot.ImmersiveAutopilot;
import com.immersiveautopilot.menu.ModMenus;
import com.immersiveautopilot.screen.RadarScreen;
import com.immersiveautopilot.screen.TowerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TOWER_MENU.get(), TowerScreen::new);
        event.register(ModMenus.RADAR_MENU.get(), RadarScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(AutopilotKeyBindings.TOGGLE_AUTOPILOT);
        event.register(AutopilotKeyBindings.CYCLE_ROUTE);
    }
}
