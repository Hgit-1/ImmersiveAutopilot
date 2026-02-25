package com.immersiveautopilot.screen;

import com.immersiveautopilot.menu.RadarMenu;
import com.immersiveautopilot.client.ClientCache;
import com.immersiveautopilot.data.AircraftSnapshot;
import com.immersiveautopilot.network.C2SRequestRadarAircraftList;
import immersive_aircraft.cobalt.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RadarScreen extends AbstractContainerScreen<RadarMenu> {
    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF101215);
        graphics.fill(leftPos + 6, topPos + 16, leftPos + imageWidth - 6, topPos + imageHeight - 6, 0xFF1B1F26);

        int outer = 0xFFBFC4C8;
        int inner = 0xFF6A7076;
        int core = 0xFF24272C;
        for (var slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 19, y + 19, outer);
            graphics.fill(x, y, x + 18, y + 18, inner);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, core);
        }

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
        int x0 = leftPos + 8;
        int y0 = topPos + 24;
        int x1 = x0 + mapSize;
        int y1 = y0 + mapSize;
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
        int range = 64 + menu.getRadar().getRangeBonus();
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

        int listX = leftPos + 80;
        int listY = topPos + 24;
        int max = Math.min(4, list.size());
        for (int i = 0; i < max; i++) {
            AircraftSnapshot snapshot = list.get(i);
            String line = snapshot.getName() + " " + (int) snapshot.getAltitude();
            graphics.drawString(font, line, listX, listY + i * 10, 0xFFFFFFFF, false);
        }
    }
}
