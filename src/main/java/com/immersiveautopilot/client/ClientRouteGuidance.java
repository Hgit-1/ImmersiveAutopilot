package com.immersiveautopilot.client;

import com.immersiveautopilot.route.RouteProgram;
import com.immersiveautopilot.route.RouteWaypoint;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ClientRouteGuidance {
    private static final double TRIGGER_RADIUS = 25.0;
    private static final double TRIGGER_RADIUS_SQ = TRIGGER_RADIUS * TRIGGER_RADIUS;
    private static final int PRIMARY_COLOR = 0xFF4FC3F7;
    private static final int BACKUP_COLOR = 0xFFB066FF;
    private static final int COMPLETE_COLOR = 0xFF53C653;

    private static boolean inAirspace = false;
    private static boolean completedInAirspace = false;
    private static int activeVehicleId = -1;
    private static RouteProgram primary;
    private static RouteProgram backup;
    private static RouteProgram active;
    private static boolean activeIsBackup = false;
    private static int completedCount = 0;

    private ClientRouteGuidance() {
    }

    public static void onAirspaceEnter(int vehicleId) {
        inAirspace = true;
        completedInAirspace = false;
        activeVehicleId = vehicleId;
    }

    public static void onAirspaceExit(int vehicleId) {
        if (activeVehicleId != -1 && vehicleId != -1 && activeVehicleId != vehicleId) {
            return;
        }
        clearRoutes();
        inAirspace = false;
        completedInAirspace = false;
        activeVehicleId = -1;
    }

    public static boolean shouldSuppressOffers(int vehicleId) {
        if (!inAirspace || !completedInAirspace) {
            return false;
        }
        return activeVehicleId == -1 || activeVehicleId == vehicleId;
    }

    public static void acceptRoutes(int vehicleId, RouteProgram primaryRoute, RouteProgram backupRoute) {
        if (!inAirspace) {
            return;
        }
        activeVehicleId = vehicleId;
        primary = primaryRoute;
        backup = backupRoute;
        activeIsBackup = primary == null && backup != null;
        active = primary != null ? primary : backup;
        completedCount = 0;
        completedInAirspace = false;
        XaeroBridge.syncTemporaryWaypoints(primary, backup, activeIsBackup ? 0 : completedCount, activeIsBackup ? completedCount : 0);
    }

    public static void tick() {
        if (!inAirspace || completedInAirspace || active == null) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null || player.getVehicle() == null) {
            return;
        }
        if (activeVehicleId != -1 && player.getVehicle().getId() != activeVehicleId) {
            return;
        }
        List<RouteWaypoint> points = active.getWaypoints();
        if (completedCount >= points.size()) {
            finishRoute();
            return;
        }
        RouteWaypoint target = points.get(completedCount);
        if (!target.getDimension().equals(player.level().dimension().location())) {
            return;
        }
        Vec3 pos = player.getVehicle().position();
        double dx = pos.x - target.getPos().getX();
        double dy = pos.y - target.getPos().getY();
        double dz = pos.z - target.getPos().getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq <= TRIGGER_RADIUS_SQ) {
            completedCount++;
            if (completedCount >= points.size()) {
                finishRoute();
            } else {
                XaeroBridge.syncTemporaryWaypoints(primary, backup, activeIsBackup ? 0 : completedCount, activeIsBackup ? completedCount : 0);
            }
        }
    }

    private static void finishRoute() {
        completedInAirspace = true;
        active = null;
        XaeroBridge.clearTemporaryWaypoints();
    }

    private static void clearRoutes() {
        primary = null;
        backup = null;
        active = null;
        completedCount = 0;
        XaeroBridge.clearTemporaryWaypoints();
        ClientRouteCache.setRoutes(null, null);
    }

    public static void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camPos, Level level) {
        if (!inAirspace || completedInAirspace || active == null) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null || player.getVehicle() == null) {
            return;
        }
        if (activeVehicleId != -1 && player.getVehicle().getId() != activeVehicleId) {
            return;
        }

        int baseColor = activeIsBackup ? BACKUP_COLOR : PRIMARY_COLOR;
        List<RouteWaypoint> points = active.getWaypoints();
        for (int i = 0; i < points.size(); i++) {
            RouteWaypoint wp = points.get(i);
            if (!wp.getDimension().equals(level.dimension().location())) {
                continue;
            }
            int color = i < completedCount ? COMPLETE_COLOR : baseColor;
            drawCircle(pose, buffers, camPos, Vec3.atCenterOf(wp.getPos()), TRIGGER_RADIUS, color);
        }

        if (completedCount >= points.size()) {
            return;
        }
        RouteWaypoint target = points.get(completedCount);
        if (!target.getDimension().equals(level.dimension().location())) {
            return;
        }
        Vec3 from = player.getVehicle().position();
        Vec3 to = Vec3.atCenterOf(target.getPos());
        drawArrow(pose, buffers, camPos, from, to, baseColor);
    }

    private static void drawCircle(PoseStack pose, MultiBufferSource buffers, Vec3 camPos, Vec3 center, double radius, int color) {
        int segments = 32;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        var consumer = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(center.x - camPos.x, center.y - camPos.y + 0.1, center.z - camPos.z);
        for (int i = 0; i < segments; i++) {
            double a0 = (2 * Math.PI * i) / segments;
            double a1 = (2 * Math.PI * (i + 1)) / segments;
            float x0 = (float) (Math.cos(a0) * radius);
            float z0 = (float) (Math.sin(a0) * radius);
            float x1 = (float) (Math.cos(a1) * radius);
            float z1 = (float) (Math.sin(a1) * radius);
            consumer.vertex(pose.last().pose(), x0, 0f, z0).color(r, g, b, 1f).normal(0f, 1f, 0f).endVertex();
            consumer.vertex(pose.last().pose(), x1, 0f, z1).color(r, g, b, 1f).normal(0f, 1f, 0f).endVertex();
        }
        pose.popPose();
    }

    private static void drawArrow(PoseStack pose, MultiBufferSource buffers, Vec3 camPos, Vec3 from, Vec3 to, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        var consumer = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camPos.x, -camPos.y, -camPos.z);
        consumer.vertex(pose.last().pose(), (float) from.x, (float) from.y, (float) from.z).color(r, g, b, 1f).normal(0f, 1f, 0f).endVertex();
        consumer.vertex(pose.last().pose(), (float) to.x, (float) to.y, (float) to.z).color(r, g, b, 1f).normal(0f, 1f, 0f).endVertex();

        Vec3 dir = to.subtract(from).normalize();
        Vec3 left = dir.yRot((float) Math.toRadians(150)).scale(2.0);
        Vec3 right = dir.yRot((float) Math.toRadians(-150)).scale(2.0);
        Vec3 head = to;
        Vec3 p1 = head.add(left);
        Vec3 p2 = head.add(right);
        consumer.vertex(pose.last().pose(), (float) head.x, (float) head.y, (float) head.z).color(r, g, b, 1f).normal(0f, 1f, 0f).endVertex();
        consumer.vertex(pose.last().pose(), (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, 1f).normal(0f, 1f, 0f).endVertex();
        consumer.vertex(pose.last().pose(), (float) head.x, (float) head.y, (float) head.z).color(r, g, b, 1f).normal(0f, 1f, 0f).endVertex();
        consumer.vertex(pose.last().pose(), (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, 1f).normal(0f, 1f, 0f).endVertex();
        pose.popPose();
    }
}
