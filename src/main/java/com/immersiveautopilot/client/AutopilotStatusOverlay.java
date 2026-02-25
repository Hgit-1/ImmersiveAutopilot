package com.immersiveautopilot.client;

import com.immersiveautopilot.ImmersiveAutopilot;
import com.immersiveautopilot.autopilot.AutopilotSupport;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AutopilotStatusOverlay {
    private static final int COLOR = 0xFFFFD24A;

    private AutopilotStatusOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }
        if (!(mc.player != null && mc.player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (!AutopilotSupport.isAutopilotEnabled(vehicle)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Component text = Component.translatable("hud.immersive_autopilot.autopilot_on");
        int x = 8;
        int y = 8;
        graphics.drawString(mc.font, text, x, y, COLOR, true);
    }
}
