package com.immersiveautopilot.screen;

import com.immersiveautopilot.network.C2SPilotRouteDecision;
import com.immersiveautopilot.route.RouteEntry;
import immersive_aircraft.cobalt.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, title, this.width / 2, this.height / 2 - 90, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("screen.immersive_autopilot.route_offer_from")
                .append(": ").append(operatorName), this.width / 2, this.height / 2 - 70, 0xFFFFFFFF);

        int listX = this.width / 2 - 120;
        int listY = this.height / 2 - 50;
        graphics.drawString(this.font, Component.translatable("screen.immersive_autopilot.route_offer_primary"), listX, listY - 12, 0xFFFFFFFF, true);
        graphics.drawString(this.font, Component.translatable("screen.immersive_autopilot.route_offer_backup"), listX + 160, listY - 12, 0xFFFFFFFF, true);

        for (int i = 0; i < entries.size(); i++) {
            int y = listY + i * 12;
            int color = i == selectedPrimary ? 0xFFFF4040 : 0xFFFFFFFF;
            graphics.drawString(this.font, entries.get(i).name(), listX, y, color, true);
            int bColor = i == selectedBackup ? 0xFF4FC3F7 : 0xFFB0B0B0;
            graphics.drawString(this.font, i == selectedBackup ? "[B]" : "[ ]", listX + 160, y, bColor, true);
        }

        acceptButton.active = selectedPrimary >= 0;
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = this.width / 2 - 120;
        int listY = this.height / 2 - 50;
        int listWidth = 140;
        int listHeight = entries.size() * 12;
        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            int index = (int) ((mouseY - listY) / 12);
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
