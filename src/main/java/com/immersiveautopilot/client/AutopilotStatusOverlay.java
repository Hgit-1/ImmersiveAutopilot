package com.immersiveautopilot.client;

import com.immersiveautopilot.ImmersiveAutopilot;
import com.immersiveautopilot.autopilot.AutopilotSupport;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AutopilotStatusOverlay {
    private static final int COLOR_AUTOPILOT = 0xFFFFD24A;
    private static final int COLOR_ALERT = 0xFFFF4040;
    private static final double TERRAIN_CHECK_DISTANCE = 16.0;

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
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int baseY = height - 68;

        int line = 0;
        if (AutopilotSupport.isAutopilotEnabled(vehicle)) {
            Component text = Component.translatable("hud.immersive_autopilot.autopilot_on");
            drawCentered(graphics, mc, text, baseY + line * (mc.font.lineHeight + 2), COLOR_AUTOPILOT);
            line++;
        }

        Component traffic = TrafficAlertOverlay.getActiveAlert();
        if (traffic != null) {
            drawCentered(graphics, mc, traffic, baseY + line * (mc.font.lineHeight + 2), COLOR_ALERT);
            line++;
        }

        if (AutopilotSupport.isAutopilotEnabled(vehicle) && isTerrainDanger(mc, vehicle)) {
            Component warn = Component.translatable("hud.immersive_autopilot.terrain_alert");
            drawCentered(graphics, mc, warn, baseY + line * (mc.font.lineHeight + 2), COLOR_ALERT);
        }
    }

    private static boolean isTerrainDanger(Minecraft mc, VehicleEntity vehicle) {
        Vec3 start = vehicle.position().add(0.0, Math.max(0.5, vehicle.getBbHeight() * 0.5), 0.0);
        Vec3 forward = new Vec3(vehicle.getForwardDirection().x(), vehicle.getForwardDirection().y(), vehicle.getForwardDirection().z()).normalize();
        Vec3 end = start.add(forward.scale(TERRAIN_CHECK_DISTANCE));
        BlockHitResult hit = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
        if (hit.getType() != BlockHitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        return mc.level.getBlockState(pos).isCollisionShapeFullBlock(mc.level, pos);
    }

    private static void drawCentered(GuiGraphics graphics, Minecraft mc, Component text, int y, int color) {
        int width = graphics.guiWidth();
        int x = Math.max(2, (width - mc.font.width(text)) / 2);
        graphics.drawString(mc.font, text, x, y, color, true);
    }
}
