package com.immersiveautopilot.screen;

import com.immersiveautopilot.menu.RadarMenu;
import com.immersiveautopilot.client.ClientCache;
import com.immersiveautopilot.client.TrafficAlertOverlay;
import com.immersiveautopilot.data.AircraftSnapshot;
import com.immersiveautopilot.network.C2SRequestRadarAircraftList;
import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RadarScreen extends AbstractContainerScreen<RadarMenu> {
    private static final ResourceLocation DISPENSER_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/dispenser.png");
    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");
    private static final int PANEL_WIDTH = RadarMenu.PANEL_WIDTH;
    private static final int PANEL_GAP = RadarMenu.PANEL_GAP;
    private static final int ALERT_COLOR = 0xFFFF4040;
    private static final double ALERT_DISTANCE = 20.0;
    private static final int ALERT_TIME_TICKS = 100;

    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_WIDTH + PANEL_GAP + 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = RadarMenu.BASE_X + 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int panelX = leftPos;
        int panelY = topPos;
        drawPanel(graphics, panelX, panelY, imageHeight, PANEL_WIDTH);

        int dispenserX = leftPos + PANEL_WIDTH + PANEL_GAP;
        graphics.blit(DISPENSER_TEXTURE, dispenserX, topPos, 0, 0, 176, 166, 256, 256);

        drawAircraftMap(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        graphics.drawString(font, title, leftPos + 8, topPos + 6, 0xFFFFFFFF, false);
    }

    @Override
    protected void init() {
        super.init();
        NetworkHandler.sendToServer(new C2SRequestRadarAircraftList(menu.getPos()));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (minecraft != null && minecraft.level != null && minecraft.level.getGameTime() % 20 == 0) {
            NetworkHandler.sendToServer(new C2SRequestRadarAircraftList(menu.getPos()));
        }
    }

    private void drawAircraftMap(GuiGraphics graphics) {
        int mapSize = 64;
        int x0 = leftPos + 16;
        int y0 = topPos + 20;
        int x1 = x0 + mapSize;
        int y1 = y0 + mapSize;
        graphics.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0xFF2A2F36);
        graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF0B0D10);
        graphics.fill(x0, y0, x1, y1, 0xFF0B0D10);
        for (int i = 0; i <= mapSize; i += 16) {
            graphics.hLine(x0, x1, y0 + i, 0x5522262B);
            graphics.vLine(x0 + i, y0, y1, 0x5522262B);
        }
        graphics.hLine(x0, x1, y0 + mapSize / 2, 0xFF2F343A);
        graphics.vLine(x0 + mapSize / 2, y0, y1, 0xFF2F343A);

        List<AircraftSnapshot> list = ClientCache.getAircraftList(menu.getPos());
        if (list == null || list.isEmpty()) {
            return;
        }
        int range = Math.max(1, 64 + menu.getRadar().getRangeBonus());
        double scale = range > 0 ? (mapSize / (double) (range * 2)) : 1.0;
        Vec3 center = Vec3.atCenterOf(menu.getPos());
        Set<Integer> conflicts = findTrafficConflicts(list);
        for (AircraftSnapshot snapshot : list) {
            double dx = snapshot.getPosX() - center.x;
            double dz = snapshot.getPosZ() - center.z;
            int px = x0 + (int) Math.round(mapSize / 2.0 + dx * scale);
            int pz = y0 + (int) Math.round(mapSize / 2.0 + dz * scale);
            if (px < x0 || px >= x1 || pz < y0 || pz >= y1) {
                continue;
            }
            int color = conflicts.contains(snapshot.getEntityId()) ? ALERT_COLOR : 0xFF4FC3F7;
            graphics.fill(px - 1, pz - 1, px + 2, pz + 2, color);
        }
        drawPlayerDirectionLine(graphics, x0, y0, x1, y1, center, scale, conflicts);

        int listX = leftPos + 8;
        int listY = y1 + 16;
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.aircraft"), listX, listY - 10, 0xFFE0E0E0, false);
        int max = Math.min(5, list.size());
        for (int i = 0; i < max; i++) {
            AircraftSnapshot snapshot = list.get(i);
            double speedMs = snapshot.getSpeed() * 20.0;
            String line = String.format(Locale.ROOT, "%s %d,%d,%d %.1fm/s",
                    snapshot.getName(), (int) snapshot.getPosX(), (int) snapshot.getAltitude(), (int) snapshot.getPosZ(), speedMs);
            int color = conflicts.contains(snapshot.getEntityId()) ? ALERT_COLOR : 0xFFFFFFFF;
            graphics.drawString(font, line, listX, listY + i * 10, color, false);
        }
    }

    private void drawPanel(GuiGraphics context, int x, int y, int h, int w) {
        context.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        context.fill(x, y, x + w, y + 1, 0xFFF0F0F0);
        context.fill(x, y, x + 1, y + h, 0xFFF0F0F0);
        context.fill(x, y + h - 1, x + w, y + h, 0xFF8A8A8A);
        context.fill(x + w - 1, y, x + w, y + h, 0xFF8A8A8A);
    }

    private Set<Integer> findTrafficConflicts(List<AircraftSnapshot> list) {
        Set<Integer> conflicts = new HashSet<>();
        Component alertText = null;
        int size = list.size();
        for (int i = 0; i < size; i++) {
            AircraftSnapshot a = list.get(i);
            for (int j = i + 1; j < size; j++) {
                AircraftSnapshot b = list.get(j);
                double rx = a.getPosX() - b.getPosX();
                double ry = a.getAltitude() - b.getAltitude();
                double rz = a.getPosZ() - b.getPosZ();
                double vx = a.getVelX() - b.getVelX();
                double vy = a.getVelY() - b.getVelY();
                double vz = a.getVelZ() - b.getVelZ();
                double vSq = vx * vx + vy * vy + vz * vz;
                if (vSq < 1.0E-6) {
                    continue;
                }
                double t = -(rx * vx + ry * vy + rz * vz) / vSq;
                if (t <= 0.0 || t > ALERT_TIME_TICKS) {
                    continue;
                }
                double dx = rx + vx * t;
                double dy = ry + vy * t;
                double dz = rz + vz * t;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist < ALERT_DISTANCE) {
                    conflicts.add(a.getEntityId());
                    conflicts.add(b.getEntityId());
                    if (alertText == null) {
                        alertText = Component.translatable("message.immersive_autopilot.traffic_alert", a.getName(), b.getName());
                    }
                }
            }
        }
        if (alertText != null) {
            TrafficAlertOverlay.pushAlert(alertText);
        }
        return conflicts;
    }

    private void drawPlayerDirectionLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, Vec3 center, double scale, Set<Integer> conflicts) {
        if (minecraft == null) {
            return;
        }
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        double dx = vehicle.getX() - center.x;
        double dz = vehicle.getZ() - center.z;
        int px = x0 + (int) Math.round((x1 - x0) / 2.0 + dx * scale);
        int pz = y0 + (int) Math.round((y1 - y0) / 2.0 + dz * scale);
        if (px < x0 || px >= x1 || pz < y0 || pz >= y1) {
            return;
        }
        var forward = vehicle.getForwardDirection();
        double fx = forward.x();
        double fz = forward.z();
        double len = Math.sqrt(fx * fx + fz * fz);
        if (len < 1.0E-4) {
            return;
        }
        fx /= len;
        fz /= len;
        int back = 2;
        int front = 6;
        int xStart = (int) Math.round(px - fx * back);
        int yStart = (int) Math.round(pz - fz * back);
        int xEnd = (int) Math.round(px + fx * front);
        int yEnd = (int) Math.round(pz + fz * front);
        int color = conflicts.contains(vehicle.getId()) ? ALERT_COLOR : 0xFF4FC3F7;
        drawLine(graphics, xStart, yStart, xEnd, yEnd, color);
    }

    private void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
}
