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
    private static final int ROWS = 6;
    private static final int ROW_HEIGHT = 12;

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
        this.imageWidth = 230;
        this.imageHeight = 210;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        rangeField = new EditBox(font, x + 10, y + 18, 50, 16, Component.translatable("screen.immersive_autopilot.scan_range"));
        rangeField.setValue("256");
        addRenderableWidget(rangeField);

        routeNameField = new EditBox(font, x + 120, y + 18, 90, 16, Component.translatable("screen.immersive_autopilot.route_name"));
        routeNameField.setValue(activeRoute.getName().isEmpty() ? "default" : activeRoute.getName());
        addRenderableWidget(routeNameField);

        speedField = new EditBox(font, x + 120, y + 40, 40, 16, Component.translatable("screen.immersive_autopilot.speed"));
        speedField.setValue("1.0");
        addRenderableWidget(speedField);

        holdField = new EditBox(font, x + 170, y + 40, 40, 16, Component.translatable("screen.immersive_autopilot.hold"));
        holdField.setValue("0");
        addRenderableWidget(holdField);

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.refresh_list"),
                button -> refreshAircraftList())
                .bounds(x + 10, y + 40, 80, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.bind_selected"),
                button -> bindSelected())
                .bounds(x + 10, y + 140, 100, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.unbind"),
                button -> unbind())
                .bounds(x + 120, y + 140, 60, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.add_waypoint"),
                button -> addWaypoint())
                .bounds(x + 120, y + 80, 90, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.remove_waypoint"),
                button -> removeWaypoint())
                .bounds(x + 120, y + 102, 90, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.save_preset"),
                button -> savePreset())
                .bounds(x + 120, y + 124, 90, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.load_preset"),
                button -> loadPreset())
                .bounds(x + 120, y + 146, 90, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.send_route"),
                button -> sendRoute())
                .bounds(x + 120, y + 170, 90, 18).build());

        addRenderableWidget(Button.builder(Component.literal("<"),
                button -> prevPage())
                .bounds(x + 10, y + 118, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal(">"),
                button -> nextPage())
                .bounds(x + 40, y + 118, 20, 18).build());

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
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1E1E1E);
        graphics.drawString(font, title, leftPos + 8, topPos + 6, 0xFFFFFF, false);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.scan_range"), leftPos + 10, topPos + 6 + 16, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.route_name"), leftPos + 120, topPos + 6 + 16, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.speed"), leftPos + 120, topPos + 32, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.hold"), leftPos + 170, topPos + 32, 0xA0A0A0, false);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.bound_aircraft"), leftPos + 10, topPos + 160, 0xA0A0A0, false);
        String boundText = boundName == null || boundName.isBlank() ? Component.translatable("screen.immersive_autopilot.no_bound_aircraft").getString() : boundName;
        graphics.drawString(font, boundText, leftPos + 10, topPos + 172, 0xFFFFFF, false);

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
        int listX = leftPos + 10;
        int listY = topPos + 72;
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.aircraft"), listX, topPos + 60, 0xA0A0A0, false);

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
        int listX = leftPos + 120;
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.waypoints"), listX, topPos + 60, 0xA0A0A0, false);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = leftPos + 10;
        int listY = topPos + 72;
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
