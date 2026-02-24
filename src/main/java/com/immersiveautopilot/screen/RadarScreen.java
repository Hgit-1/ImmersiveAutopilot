package com.immersiveautopilot.screen;

import com.immersiveautopilot.menu.RadarMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

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
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        graphics.drawString(font, title, leftPos + 8, topPos + 6, 0xFFFFFFFF, false);
    }
}
