package com.immersiveautopilot.screen;

import com.immersiveautopilot.network.C2SPilotRouteDecision;
import com.immersiveautopilot.route.RouteEntry;
import immersive_aircraft.cobalt.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;

public class RouteOfferScreen extends Screen {
    private final int vehicleId;
    private final String operatorName;
    private final List<RouteEntry> entries;

    private int selectedPrimary = -1;
    private int selectedBackup = -1;

    private Button acceptButton;
    private Button declineButton;

    public RouteOfferScreen(int vehicleId, String operatorName, List<RouteEntry> entries) {
        super(Component.translatable("screen.immersive_autopilot.route_offer_title"));
        this.vehicleId = vehicleId;
        this.operatorName = operatorName;
        this.entries = entries;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        acceptButton = Button.builder(Component.translatable("screen.immersive_autopilot.route_offer_accept"),
                button -> {
                    String primary = selectedPrimary >= 0 ? entries.get(selectedPrimary).name() : "";
                    String backup = selectedBackup >= 0 ? entries.get(selectedBackup).name() : "";
                    com.immersiveautopilot.client.ClientRouteCache.setRoutes(
                            vehicleId,
                            selectedPrimary >= 0 ? entries.get(selectedPrimary).program() : null,
                            selectedBackup >= 0 ? entries.get(selectedBackup).program() : null
                    );
                    NetworkHandler.sendToServer(new C2SPilotRouteDecision(vehicleId, true, primary, backup));
                    Minecraft.getInstance().setScreen(null);
                }).bounds(centerX - 90, centerY + 70, 80, 20).build();
        acceptButton.active = selectedPrimary >= 0;
        addRenderableWidget(acceptButton);

        declineButton = Button.builder(Component.translatable("screen.immersive_autopilot.route_offer_decline"),
                button -> {
                    NetworkHandler.sendToServer(new C2SPilotRouteDecision(vehicleId, false, "", ""));
                    Minecraft.getInstance().setScreen(null);
                }).bounds(centerX + 10, centerY + 70, 80, 20).build();
        addRenderableWidget(declineButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Avoid background blur from other UI mods.
        int panelLeft = this.width / 2 - 140;
        int panelRight = this.width / 2 + 140;
        int panelTop = this.height / 2 - 100;
        int panelBottom = this.height / 2 + 90;
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE0101214);

        int listX = this.width / 2 - 120;
        int listY = this.height / 2 - 50;

        for (int i = 0; i < entries.size(); i++) {
            int y = listY + i * 14;
            int color = i == selectedPrimary ? 0xFFFF4040 : 0xFFFFFFFF;
            graphics.drawString(this.font, entries.get(i).name(), listX, y, color, true);
            int bColor = i == selectedBackup ? 0xFF4FC3F7 : 0xFFB0B0B0;
            graphics.drawString(this.font, i == selectedBackup ? "[B]" : "[ ]", listX + 160, y, bColor, true);
        }

        acceptButton.active = selectedPrimary >= 0;
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, title.copy().withStyle(ChatFormatting.BOLD), this.width / 2, this.height / 2 - 90, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("screen.immersive_autopilot.route_offer_from")
                .append(": ").append(operatorName).withStyle(ChatFormatting.BOLD), this.width / 2, this.height / 2 - 70, 0xFFFFFFFF);
        graphics.drawString(this.font, Component.translatable("screen.immersive_autopilot.route_offer_primary").withStyle(ChatFormatting.BOLD), listX, listY - 14, 0xFFFFFFFF, true);
        graphics.drawString(this.font, Component.translatable("screen.immersive_autopilot.route_offer_backup").withStyle(ChatFormatting.BOLD), listX + 160, listY - 14, 0xFFFFFFFF, true);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally blank to avoid background blur.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = this.width / 2 - 120;
        int listY = this.height / 2 - 50;
        int listWidth = 140;
        int listHeight = entries.size() * 14;
        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            int index = (int) ((mouseY - listY) / 14);
            if (index >= 0 && index < entries.size()) {
                if (button == 0) {
                    selectedPrimary = index;
                    if (selectedBackup == index) {
                        selectedBackup = -1;
                    }
                    return true;
                }
                if (button == 1) {
                    selectedBackup = index;
                    if (selectedPrimary == -1) {
                        selectedPrimary = index;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
