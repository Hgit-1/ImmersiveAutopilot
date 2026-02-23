package com.immersiveautopilot.screen;

import com.immersiveautopilot.client.ClientCache;
import com.immersiveautopilot.data.AircraftSnapshot;
import com.immersiveautopilot.data.TowerState;
import com.immersiveautopilot.menu.TowerMenu;
import com.immersiveautopilot.network.C2SBindAircraft;
import com.immersiveautopilot.network.C2SLoadPreset;
import com.immersiveautopilot.network.C2SRequestAircraftList;
import com.immersiveautopilot.network.C2SRequestTowerState;
import com.immersiveautopilot.network.C2SSavePreset;
import com.immersiveautopilot.network.C2SSetTowerRange;
import com.immersiveautopilot.network.C2SSendRouteToAircraft;
import com.immersiveautopilot.network.C2SUnbindAircraft;
import com.immersiveautopilot.route.RouteProgram;
import com.immersiveautopilot.route.RouteWaypoint;
import immersive_aircraft.cobalt.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TowerScreen extends AbstractContainerScreen<TowerMenu> {
    private static final int ROWS = 5;
    private static final int ROW_HEIGHT = 12;
    private static final int LIST_Y_OFFSET = 92;
    private static final int LEFT_X_OFFSET = 10;
    private static final int RIGHT_X_OFFSET = 130;
    private static final int RIGHT_BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 18;

    private EditBox rangeField;
    private EditBox routeNameField;
    private EditBox speedField;
    private EditBox holdField;

    private List<AircraftSnapshot> aircraft = new ArrayList<>();
    private int selectedIndex = -1;
    private int page = 0;

    private UUID boundUuid;
    private String boundName = "";

    private RouteProgram activeRoute = new RouteProgram("default");

    public TowerScreen(TowerMenu menu, net.minecraft.world.entity.player.Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 248;
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        rangeField = new EditBox(font, x + LEFT_X_OFFSET, y + 22, 60, 16, Component.translatable("screen.immersive_autopilot.scan_range"));
        rangeField.setValue("256");
        addRenderableWidget(rangeField);

        routeNameField = new EditBox(font, x + RIGHT_X_OFFSET, y + 22, 100, 16, Component.translatable("screen.immersive_autopilot.route_name"));
        routeNameField.setValue(activeRoute.getName().isEmpty() ? "default" : activeRoute.getName());
        addRenderableWidget(routeNameField);

        speedField = new EditBox(font, x + RIGHT_X_OFFSET, y + 44, 45, 16, Component.translatable("screen.immersive_autopilot.speed"));
        speedField.setValue("1.0");
        addRenderableWidget(speedField);

        holdField = new EditBox(font, x + RIGHT_X_OFFSET + 55, y + 44, 45, 16, Component.translatable("screen.immersive_autopilot.hold"));
        holdField.setValue("0");
        addRenderableWidget(holdField);

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.refresh_list"),
                button -> refreshAircraftList())
                .bounds(x + LEFT_X_OFFSET, y + 44, 90, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.bind_selected"),
                button -> bindSelected())
                .bounds(x + LEFT_X_OFFSET, y + 170, 110, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.unbind"),
                button -> unbind())
                .bounds(x + RIGHT_X_OFFSET, y + 156, 70, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.add_waypoint"),
                button -> addWaypoint())
                .bounds(x + RIGHT_X_OFFSET, y + 70, 110, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.remove_waypoint"),
                button -> removeWaypoint())
                .bounds(x + RIGHT_X_OFFSET, y + 92, 110, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.save_preset"),
                button -> savePreset())
                .bounds(x + RIGHT_X_OFFSET, y + 114, 110, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.load_preset"),
                button -> loadPreset())
                .bounds(x + RIGHT_X_OFFSET, y + 136, 110, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.send_route"),
                button -> sendRoute())
                .bounds(x + RIGHT_X_OFFSET, y + 182, RIGHT_BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("<"),
                button -> prevPage())
                .bounds(x + LEFT_X_OFFSET, y + 70, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal(">"),
                button -> nextPage())
                .bounds(x + LEFT_X_OFFSET + 30, y + 70, 20, 18).build());

        NetworkHandler.sendToServer(new C2SRequestTowerState(menu.getPos()));
        refreshAircraftList();
    }

    private void refreshAircraftList() {
        int range = parseInt(rangeField.getValue(), 256);
        NetworkHandler.sendToServer(new C2SSetTowerRange(menu.getPos(), range));
        NetworkHandler.sendToServer(new C2SRequestAircraftList(menu.getPos()));
    }

    private void bindSelected() {
        if (selectedIndex < 0 || selectedIndex >= aircraft.size()) {
            return;
        }
        AircraftSnapshot snapshot = aircraft.get(selectedIndex);
        NetworkHandler.sendToServer(new C2SBindAircraft(menu.getPos(), snapshot.getEntityId()));
    }

    private void unbind() {
        NetworkHandler.sendToServer(new C2SUnbindAircraft(menu.getPos()));
    }

    private void addWaypoint() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float speed = parseFloat(speedField.getValue(), 1.0f);
        int hold = parseInt(holdField.getValue(), 0);
        RouteProgram program = buildProgramFromUi();
        program.addWaypoint(new RouteWaypoint(player.blockPosition(), player.level().dimension().location(), speed, hold));
        activeRoute = program;
    }

    private void removeWaypoint() {
        RouteProgram program = buildProgramFromUi();
        program.removeLastWaypoint();
        activeRoute = program;
    }

    private void savePreset() {
        String name = routeNameField.getValue();
        if (name == null || name.isBlank()) {
            return;
        }
        RouteProgram program = buildProgramFromUi();
        NetworkHandler.sendToServer(new C2SSavePreset(menu.getPos(), name, program));
    }

    private void loadPreset() {
        String name = routeNameField.getValue();
        if (name == null || name.isBlank()) {
            return;
        }
        NetworkHandler.sendToServer(new C2SLoadPreset(menu.getPos(), name));
    }

    private void sendRoute() {
        int targetId = resolveTargetEntityId();
        if (targetId == -1) {
            return;
        }
        RouteProgram program = buildProgramFromUi();
        NetworkHandler.sendToServer(new C2SSendRouteToAircraft(menu.getPos(), targetId, program));
    }

    private int resolveTargetEntityId() {
        if (selectedIndex >= 0 && selectedIndex < aircraft.size()) {
            return aircraft.get(selectedIndex).getEntityId();
        }
        if (boundUuid != null) {
            for (AircraftSnapshot snapshot : aircraft) {
                if (boundUuid.equals(snapshot.getUuid())) {
                    return snapshot.getEntityId();
                }
            }
        }
        return -1;
    }

    private RouteProgram buildProgramFromUi() {
        String name = routeNameField.getValue();
        if (name == null || name.isBlank()) {
            name = "default";
        }
        return new RouteProgram(name, new ArrayList<>(activeRoute.getWaypoints()));
    }

    private void prevPage() {
        if (page > 0) {
            page--;
        }
    }

    private void nextPage() {
        int maxPage = Math.max(0, (aircraft.size() - 1) / ROWS);
        if (page < maxPage) {
            page++;
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        TowerState state = ClientCache.getTowerState(menu.getPos());
        if (state != null) {
            boundUuid = state.getBoundAircraft();
            boundName = state.getBoundName();
            if (!rangeField.isFocused()) {
                rangeField.setValue(Integer.toString(state.getScanRange()));
            }
            if (state.getActiveRoute() != null) {
                String desiredName = routeNameField.getValue();
                boolean shouldUpdate = activeRoute.getWaypoints().isEmpty()
                        || (desiredName != null && desiredName.equals(state.getActiveRoute().getName()));
                if (shouldUpdate) {
                    activeRoute = state.getActiveRoute();
                    if (!routeNameField.isFocused()) {
                        routeNameField.setValue(activeRoute.getName());
                    }
                }
            }
        }

        List<AircraftSnapshot> newList = ClientCache.getAircraftList(menu.getPos());
        if (newList != aircraft) {
            aircraft = newList;
            if (selectedIndex >= aircraft.size()) {
                selectedIndex = -1;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF0121418);
        graphics.drawString(font, title, leftPos + 8, topPos + 6, 0xFFFFFF, false);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.scan_range"), leftPos + LEFT_X_OFFSET, topPos + 12, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.route_name"), leftPos + RIGHT_X_OFFSET, topPos + 12, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.speed"), leftPos + RIGHT_X_OFFSET, topPos + 36, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.hold"), leftPos + RIGHT_X_OFFSET + 55, topPos + 36, 0xFFFFFF, false);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.bound_aircraft"), leftPos + LEFT_X_OFFSET, topPos + 150, 0xFFFFFF, false);
        String boundText = boundName == null || boundName.isBlank() ? Component.translatable("screen.immersive_autopilot.no_bound_aircraft").getString() : boundName;
        graphics.drawString(font, boundText, leftPos + LEFT_X_OFFSET, topPos + 162, 0xFFFFFF, false);

        drawAircraftList(graphics);
        drawRouteList(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Background is drawn in render().
    }

    private void drawAircraftList(GuiGraphics graphics) {
        int listX = leftPos + LEFT_X_OFFSET;
        int listY = topPos + LIST_Y_OFFSET;
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.aircraft"), listX, topPos + 58, 0xFFFFFF, false);

        int start = page * ROWS;
        for (int i = 0; i < ROWS; i++) {
            int index = start + i;
            int y = listY + i * ROW_HEIGHT;
            if (index >= aircraft.size()) {
                continue;
            }
            AircraftSnapshot snapshot = aircraft.get(index);
            int color = index == selectedIndex ? 0xFFFFA000 : 0xFFE0E0E0;
            String line = snapshot.getName() + " [" + (int) snapshot.getDistance() + "m]";
            graphics.drawString(font, line, listX, y, color, false);
        }
    }

    private void drawRouteList(GuiGraphics graphics) {
        int listX = leftPos + RIGHT_X_OFFSET;
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.waypoints"), listX, topPos + 60, 0xFFFFFF, false);
        int listY = topPos + 60 + 14;
        List<RouteWaypoint> points = activeRoute.getWaypoints();
        int max = Math.min(points.size(), 6);
        for (int i = 0; i < max; i++) {
            RouteWaypoint wp = points.get(i);
            String line = (i + 1) + ": " + wp.getPos().getX() + "," + wp.getPos().getY() + "," + wp.getPos().getZ() + " s=" + wp.getSpeed() + " h=" + wp.getHoldSeconds();
            graphics.drawString(font, line, listX, listY + i * ROW_HEIGHT, 0xFFE0E0E0, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Suppress default inventory labels.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = leftPos + LEFT_X_OFFSET;
        int listY = topPos + LIST_Y_OFFSET;
        int listWidth = 100;
        int listHeight = ROWS * ROW_HEIGHT;
        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            int index = (int) ((mouseY - listY) / ROW_HEIGHT) + page * ROWS;
            if (index >= 0 && index < aircraft.size()) {
                selectedIndex = index;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
