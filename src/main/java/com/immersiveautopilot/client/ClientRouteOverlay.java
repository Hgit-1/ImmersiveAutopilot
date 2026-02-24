package com.immersiveautopilot.client;

import com.immersiveautopilot.ImmersiveAutopilot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientRouteOverlay {
    private static final int PRIMARY_COLOR = 0xFF4FC3F7;
    private static final int BACKUP_COLOR = 0xFF9AA7B0;

    private ClientRouteOverlay() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // HUD route markers disabled; use an external map mod (e.g., Xaero's) if needed.
    }

    private static void renderRoute(PoseStack pose, MultiBufferSource buffers, Font font, Vec3 camPos, Level level, RouteProgram program, int color) {
        int index = 1;
        for (RouteWaypoint wp : program.getWaypoints()) {
            if (!wp.getDimension().equals(level.dimension().location())) {
                index++;
                continue;
            }
            Vec3 pos = Vec3.atCenterOf(wp.getPos());
            String label = "WP" + index + " Y=" + wp.getPos().getY();
            drawWorldLabel(pose, buffers, font, camPos, pos, label, color);
            index++;
        }
    }

    private static void drawWorldLabel(PoseStack pose, MultiBufferSource buffers, Font font, Vec3 camPos, Vec3 pos, String text, int color) {
        pose.pushPose();
        pose.translate(pos.x - camPos.x, pos.y - camPos.y + 1.5, pos.z - camPos.z);
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        pose.scale(-0.025f, -0.025f, 0.025f);
        float x = -font.width(text) / 2f;
        font.drawInBatch(text, x, 0, color, true, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 15728880);
        pose.popPose();
    }
}
