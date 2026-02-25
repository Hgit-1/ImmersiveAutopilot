package com.immersiveautopilot.screen;

import com.immersiveautopilot.client.AutoRouteClient;
import com.immersiveautopilot.client.ClientRouteGuidance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AutoRouteChoiceScreen extends Screen {
    private final int vehicleId;
    private final List<AutoRouteClient.Entry> entries;
    private int selected = 0;

    public AutoRouteChoiceScreen(int vehicleId, List<AutoRouteClient.Entry> entries) {
        super(Component.translatable("screen.immersive_autopilot.auto_route_choice_title"));
        this.vehicleId = vehicleId;
        this.entries = entries == null ? List.of() : entries;
    }

    @Override
    protected void init() {
        int boxWidth = 220;
        int boxHeight = 18;
        int startX = (width - boxWidth) / 2;
        int startY = height / 2 - 40;
        for (int i = 0; i < entries.size(); i++) {
            int y = startY + i * (boxHeight + 6);
            int idx = i;
            String label = entries.get(i).label();
            String name = entries.get(i).routeName();
            String title = label == null || label.isBlank() ? name : label + " - " + name;
            addRenderableWidget(Button.builder(Component.literal(title), button -> select(idx))
                    .bounds(startX, y, boxWidth, boxHeight).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.auto_route_choice_confirm"),
                button -> accept())
                .bounds(startX, startY + entries.size() * (boxHeight + 6) + 6, boxWidth, 18).build());
    }

    private void select(int index) {
        selected = Math.max(0, Math.min(entries.size() - 1, index));
    }

    private void accept() {
        if (entries.isEmpty()) {
            Minecraft.getInstance().setScreen(null);
            return;
        }
        String name = entries.get(selected).routeName();
        if (name == null || name.isBlank()) {
            Minecraft.getInstance().setScreen(null);
            return;
        }
        String operatorName = entries.get(selected).label();
        ClientRouteGuidance.acceptChosenRoute(vehicleId, name, operatorName);
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 70, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
