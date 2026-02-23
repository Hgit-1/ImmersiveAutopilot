package com.immersiveautopilot.client;

import com.immersiveautopilot.ImmersiveAutopilot;
import com.immersiveautopilot.menu.ModMenus;
import com.immersiveautopilot.screen.RouteOfferScreen;
import com.immersiveautopilot.screen.TowerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod.EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TOWER_MENU.get(), TowerScreen::new);
    }
}
