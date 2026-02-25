package com.immersiveautopilot.client;

import com.immersiveautopilot.ImmersiveAutopilot;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TrafficAlertOverlay {
    private static final long ALERT_DURATION_MS = 3000L;
    private static long alertUntilMs = 0L;
    private static Component alertText = Component.empty();

    private TrafficAlertOverlay() {
    }

    public static void pushAlert(Component text) {
        if (text == null) {
            return;
        }
        alertText = text;
        alertUntilMs = Util.getMillis() + ALERT_DURATION_MS;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (Util.getMillis() > alertUntilMs) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int x = Math.max(2, (width - mc.font.width(alertText)) / 2);
        graphics.drawString(mc.font, alertText, x, 6, 0xFFFF4040, true);
    }
}
