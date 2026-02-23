package com.immersiveautopilot.screen;

import com.immersiveautopilot.network.C2SPilotRouteDecision;
import com.immersiveautopilot.route.RouteProgram;
import immersive_aircraft.cobalt.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RouteOfferScreen extends Screen {
    private final int vehicleId;
    private final String operatorName;
    private final RouteProgram program;

    public RouteOfferScreen(int vehicleId, String operatorName, RouteProgram program) {
        super(Component.translatable("screen.immersive_autopilot.route_offer_title"));
        this.vehicleId = vehicleId;
        this.operatorName = operatorName;
        this.program = program;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.route_offer_accept"),
                button -> {
                    NetworkHandler.sendToServer(new C2SPilotRouteDecision(vehicleId, true));
                    Minecraft.getInstance().setScreen(null);
                }).bounds(centerX - 80, centerY + 20, 70, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.route_offer_decline"),
                button -> {
                    NetworkHandler.sendToServer(new C2SPilotRouteDecision(vehicleId, false));
                    Minecraft.getInstance().setScreen(null);
                }).bounds(centerX + 10, centerY + 20, 70, 20).build());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("screen.immersive_autopilot.route_offer_from")
                .append(": ").append(operatorName), this.width / 2, this.height / 2 - 10, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("screen.immersive_autopilot.route_offer_points")
                .append(": ").append(Integer.toString(program.getWaypoints().size())), this.width / 2, this.height / 2 + 2, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
