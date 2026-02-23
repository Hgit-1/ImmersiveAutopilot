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
import com.immersiveautopilot.network.C2SSetTowerConfig;
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
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TowerScreen extends AbstractContainerScreen<TowerMenu> {
    private static final int ROWS = 4;
    private static final int ROW_HEIGHT = 12;

    private static final int LEFT_X = 12;
    private static final int RIGHT_X = 170;

    private static final int GRID_SIZE = 128;
    private static final int GRID_X = RIGHT_X;
    private static final int GRID_Y = 182;

    private EditBox towerNameField;
    private EditBox rangeField;
    private EditBox routeNameField;
    private EditBox speedField;
    private EditBox holdField;
    private EditBox autoRequestField;
    private EditBox enterField;
    private EditBox exitField;

    private Button targetModeButton;

    private List<AircraftSnapshot> aircraft = new ArrayList<>();
    private int selectedIndex = -1;
    private int page = 0;

    private UUID boundUuid;
    private String boundName = "";

    private RouteProgram activeRoute = new RouteProgram("default");

    private int gridCenterX;
    private int gridCenterZ;
    private boolean draggingGrid = false;
    private double dragStartX;
    private double dragStartY;
    private int dragStartCenterX;
    private int dragStartCenterZ;

    private boolean targetAllInRange = false;

    public TowerScreen(TowerMenu menu, net.minecraft.world.entity.player.Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = 400;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        towerNameField = new EditBox(font, x + LEFT_X, y + 24, 120, 16, Component.translatable("screen.immersive_autopilot.tower_name"));
        towerNameField.setValue("default_tower");
        addRenderableWidget(towerNameField);

        rangeField = new EditBox(font, x + LEFT_X, y + 50, 60, 16, Component.translatable("screen.immersive_autopilot.scan_range"));
        rangeField.setValue("256");
        addRenderableWidget(rangeField);

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.refresh_list"),
                button -> refreshAircraftList())
                .bounds(x + LEFT_X + 70, y + 50, 62, 16).build());

        addRenderableWidget(Button.builder(Component.literal("<"),
                button -> prevPage())
                .bounds(x + LEFT_X, y + 72, 22, 16).build());

        addRenderableWidget(Button.builder(Component.literal(">"),
                button -> nextPage())
                .bounds(x + LEFT_X + 28, y + 72, 22, 16).build());

        routeNameField = new EditBox(font, x + RIGHT_X, y + 24, 120, 16, Component.translatable("screen.immersive_autopilot.route_name"));
        routeNameField.setValue(activeRoute.getName().isEmpty() ? "default" : activeRoute.getName());
        addRenderableWidget(routeNameField);

        speedField = new EditBox(font, x + RIGHT_X, y + 50, 50, 16, Component.translatable("screen.immersive_autopilot.speed"));
        speedField.setValue("1.0");
        addRenderableWidget(speedField);

        holdField = new EditBox(font, x + RIGHT_X + 60, y + 50, 50, 16, Component.translatable("screen.immersive_autopilot.hold"));
        holdField.setValue("0");
        addRenderableWidget(holdField);

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.add_waypoint"),
                button -> addWaypoint())
                .bounds(x + RIGHT_X, y + 74, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.remove_waypoint"),
                button -> removeWaypoint())
                .bounds(x + RIGHT_X, y + 96, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.save_preset"),
                button -> savePreset())
                .bounds(x + RIGHT_X, y + 118, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.load_preset"),
                button -> loadPreset())
                .bounds(x + RIGHT_X, y + 140, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.send_route"),
                button -> sendRoute())
                .bounds(x + RIGHT_X, y + 162, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.bind_selected"),
                button -> bindSelected())
                .bounds(x + LEFT_X, y + 212, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.unbind"),
                button -> unbind())
                .bounds(x + LEFT_X, y + 234, 120, 18).build());

        targetModeButton = Button.builder(Component.translatable("screen.immersive_autopilot.target_mode_bound"),
                button -> toggleTargetMode())
                .bounds(x + LEFT_X, y + 256, 120, 18).build();
        addRenderableWidget(targetModeButton);

        autoRequestField = new EditBox(font, x + LEFT_X, y + 328, 296, 16, Component.translatable("screen.immersive_autopilot.auto_request"));
        autoRequestField.setValue("Auto request from {tower}");
        addRenderableWidget(autoRequestField);

        enterField = new EditBox(font, x + LEFT_X, y + 350, 296, 16, Component.translatable("screen.immersive_autopilot.enter_text"));
        enterField.setValue("Entering {tower}");
        addRenderableWidget(enterField);

        exitField = new EditBox(font, x + LEFT_X, y + 372, 296, 16, Component.translatable("screen.immersive_autopilot.exit_text"));
        exitField.setValue("Leaving {tower}");
        addRenderableWidget(exitField);

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.apply_config"),
                button -> applyConfig())
                .bounds(x + LEFT_X, y + 276, 120, 16).build());

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            gridCenterX = player.blockPosition().getX();
            gridCenterZ = player.blockPosition().getZ();
        }

        NetworkHandler.sendToServer(new C2SRequestTowerState(menu.getPos()));
        refreshAircraftList();
    }

    private void refreshAircraftList() {
        int range = parseInt(rangeField.getValue(), 256);
        NetworkHandler.sendToServer(new C2SSetTowerRange(menu.getPos(), range));
        NetworkHandler.sendToServer(new C2SRequestAircraftList(menu.getPos()));
    }

    private void applyConfig() {
        NetworkHandler.sendToServer(new C2SSetTowerConfig(
                menu.getPos(),
                towerNameField.getValue(),
                autoRequestField.getValue(),
                enterField.getValue(),
                exitField.getValue(),
                targetAllInRange
        ));
    }

    private void toggleTargetMode() {
        targetAllInRange = !targetAllInRange;
        updateTargetButtonLabel();
        applyConfig();
    }

    private void updateTargetButtonLabel() {
        if (targetAllInRange) {
            targetModeButton.setMessage(Component.translatable("screen.immersive_autopilot.target_mode_all"));
        } else {
            targetModeButton.setMessage(Component.translatable("screen.immersive_autopilot.target_mode_bound"));
        }
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
            targetAllInRange = state.isTargetAllInRange();
            updateTargetButtonLabel();
            targetModeButton.visible = state.isPowered();
            targetModeButton.active = state.isPowered();
            if (!towerNameField.isFocused()) {
                towerNameField.setValue(state.getTowerName());
            }
            if (!rangeField.isFocused()) {
                rangeField.setValue(Integer.toString(state.getScanRange()));
            }
            if (!autoRequestField.isFocused()) {
                autoRequestField.setValue(state.getAutoRequestText());
            }
            if (!enterField.isFocused()) {
                enterField.setValue(state.getEnterText());
            }
            if (!exitField.isFocused()) {
                exitField.setValue(state.getExitText());
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

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.tower_name"), leftPos + LEFT_X, topPos + 12, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.scan_range"), leftPos + LEFT_X, topPos + 38, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.route_name"), leftPos + RIGHT_X, topPos + 12, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.speed"), leftPos + RIGHT_X, topPos + 38, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.hold"), leftPos + RIGHT_X + 60, topPos + 38, 0xFFFFFF, false);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.aircraft"), leftPos + LEFT_X, topPos + 92, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.waypoints"), leftPos + RIGHT_X, topPos + 92, 0xFFFFFF, false);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.bound_aircraft"), leftPos + LEFT_X, topPos + 196, 0xFFFFFF, false);
        String boundText = boundName == null || boundName.isBlank() ? Component.translatable("screen.immersive_autopilot.no_bound_aircraft").getString() : boundName;
        graphics.drawString(font, boundText, leftPos + LEFT_X, topPos + 206, 0xFFFFFF, false);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.auto_request"), leftPos + LEFT_X, topPos + 316, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.enter_text"), leftPos + LEFT_X, topPos + 338, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.exit_text"), leftPos + LEFT_X, topPos + 360, 0xFFFFFF, false);

        drawAircraftList(graphics);
        drawRouteList(graphics);
        drawGrid(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Background is drawn in render().
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Suppress default inventory labels.
    }

    private void drawAircraftList(GuiGraphics graphics) {
        int listX = leftPos + LEFT_X;
        int listY = topPos + 112;

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
        int listX = leftPos + RIGHT_X;
        int listY = topPos + 112;
        List<RouteWaypoint> points = activeRoute.getWaypoints();
        int max = Math.min(points.size(), 4);
        for (int i = 0; i < max; i++) {
            RouteWaypoint wp = points.get(i);
            String line = (i + 1) + ": " + wp.getPos().getX() + "," + wp.getPos().getZ();
            graphics.drawString(font, line, listX, listY + i * ROW_HEIGHT, 0xFFE0E0E0, false);
        }
    }

    private void drawGrid(GuiGraphics graphics) {
        int x0 = leftPos + GRID_X;
        int y0 = topPos + GRID_Y;
        int x1 = x0 + GRID_SIZE;
        int y1 = y0 + GRID_SIZE;

        graphics.fill(x0, y0, x1, y1, 0xFF0E0F12);
        for (int i = 0; i <= GRID_SIZE; i += 16) {
            graphics.hLine(x0, x1, y0 + i, 0xFF22262B);
            graphics.vLine(x0 + i, y0, y1, 0xFF22262B);
        }
        graphics.hLine(x0, x1, y0 + GRID_SIZE / 2, 0xFF2F343A);
        graphics.vLine(x0 + GRID_SIZE / 2, y0, y1, 0xFF2F343A);

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Level level = player.level();
        List<RouteWaypoint> points = activeRoute.getWaypoints();
        int lastPx = 0;
        int lastPz = 0;
        boolean hasLast = false;
        for (RouteWaypoint wp : points) {
            if (!wp.getDimension().equals(level.dimension().location())) {
                continue;
            }
            int dx = wp.getPos().getX() - gridCenterX;
            int dz = wp.getPos().getZ() - gridCenterZ;
            int px = x0 + GRID_SIZE / 2 + dx;
            int pz = y0 + GRID_SIZE / 2 + dz;
            if (px < x0 || px >= x1 || pz < y0 || pz >= y1) {
                hasLast = false;
                continue;
            }
            graphics.fill(px - 1, pz - 1, px + 2, pz + 2, 0xFFFFA000);
            if (hasLast) {
                drawLine(graphics, lastPx, lastPz, px, pz, 0xFF6FA8DC);
            }
            lastPx = px;
            lastPz = pz;
            hasLast = true;
        }
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideAircraftList(mouseX, mouseY)) {
            int index = getAircraftIndex(mouseX, mouseY);
            if (index >= 0 && index < aircraft.size()) {
                selectedIndex = index;
                return true;
            }
        }
        if (isInsideGrid(mouseX, mouseY)) {
            if (button == 0) {
                addWaypointFromGrid(mouseX, mouseY);
                return true;
            }
            if (button == 1) {
                startDrag(mouseX, mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1 && draggingGrid) {
            draggingGrid = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingGrid && button == 1) {
            int dx = (int) (mouseX - dragStartX);
            int dz = (int) (mouseY - dragStartY);
            gridCenterX = dragStartCenterX - dx;
            gridCenterZ = dragStartCenterZ - dz;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void startDrag(double mouseX, double mouseY) {
        draggingGrid = true;
        dragStartX = mouseX;
        dragStartY = mouseY;
        dragStartCenterX = gridCenterX;
        dragStartCenterZ = gridCenterZ;
    }

    private void addWaypointFromGrid(double mouseX, double mouseY) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int x0 = leftPos + GRID_X;
        int y0 = topPos + GRID_Y;
        int gridX = (int) mouseX - x0 - GRID_SIZE / 2;
        int gridZ = (int) mouseY - y0 - GRID_SIZE / 2;
        int worldX = gridCenterX + gridX;
        int worldZ = gridCenterZ + gridZ;
        float speed = parseFloat(speedField.getValue(), 1.0f);
        int hold = parseInt(holdField.getValue(), 0);
        RouteProgram program = buildProgramFromUi();
        program.addWaypoint(new RouteWaypoint(new net.minecraft.core.BlockPos(worldX, player.blockPosition().getY(), worldZ),
                player.level().dimension().location(), speed, hold));
        activeRoute = program;
    }

    private boolean isInsideGrid(double mouseX, double mouseY) {
        int x0 = leftPos + GRID_X;
        int y0 = topPos + GRID_Y;
        return mouseX >= x0 && mouseX <= x0 + GRID_SIZE && mouseY >= y0 && mouseY <= y0 + GRID_SIZE;
    }

    private boolean isInsideAircraftList(double mouseX, double mouseY) {
        int listX = leftPos + LEFT_X;
        int listY = topPos + 112;
        int listWidth = 120;
        int listHeight = ROWS * ROW_HEIGHT;
        return mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight;
    }

    private int getAircraftIndex(double mouseX, double mouseY) {
        int listY = topPos + 112;
        int row = (int) ((mouseY - listY) / ROW_HEIGHT);
        return page * ROWS + row;
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
