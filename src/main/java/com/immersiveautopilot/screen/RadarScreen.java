package com.immersiveautopilot.screen;

import com.immersiveautopilot.menu.RadarMenu;
import com.immersiveautopilot.client.ClientCache;
import com.immersiveautopilot.data.AircraftSnapshot;
import com.immersiveautopilot.network.C2SRequestRadarAircraftList;
import immersive_aircraft.cobalt.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RadarScreen extends AbstractContainerScreen<RadarMenu> {
    private static final ResourceLocation DISPENSER_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/dispenser.png");
    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");
    private static final int PANEL_WIDTH = RadarMenu.PANEL_WIDTH;
    private static final int PANEL_GAP = RadarMenu.PANEL_GAP;

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
        for (AircraftSnapshot snapshot : list) {
            double dx = snapshot.getPosX() - center.x;
            double dz = snapshot.getPosZ() - center.z;
            int px = x0 + (int) Math.round(mapSize / 2.0 + dx * scale);
            int pz = y0 + (int) Math.round(mapSize / 2.0 + dz * scale);
            if (px < x0 || px >= x1 || pz < y0 || pz >= y1) {
                continue;
            }
            graphics.fill(px - 1, pz - 1, px + 2, pz + 2, 0xFF4FC3F7);
        }

        int listX = leftPos + 8;
        int listY = y1 + 16;
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.aircraft"), listX, listY - 10, 0xFFE0E0E0, false);
        int max = Math.min(5, list.size());
        for (int i = 0; i < max; i++) {
            AircraftSnapshot snapshot = list.get(i);
            String line = snapshot.getName() + " " + (int) snapshot.getPosX() + "," + (int) snapshot.getAltitude() + "," + (int) snapshot.getPosZ();
            graphics.drawString(font, line, listX, listY + i * 10, 0xFFFFFFFF, false);
        }
    }

    private void drawPanel(GuiGraphics context, int x, int y, int h, int w) {
        context.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        context.fill(x, y, x + w, y + 1, 0xFFF0F0F0);
        context.fill(x, y, x + 1, y + h, 0xFFF0F0F0);
        context.fill(x, y + h - 1, x + w, y + h, 0xFF8A8A8A);
        context.fill(x + w - 1, y, x + w, y + h, 0xFF8A8A8A);
    }
}
