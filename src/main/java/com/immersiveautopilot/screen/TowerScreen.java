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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TowerScreen extends AbstractContainerScreen<TowerMenu> {
    private static final int ROWS = 4;
    private static final int ROW_HEIGHT = 14;

    private static final int LEFT_X = 12;
    private static final int RIGHT_X = 170;

    private static final int GRID_SIZE = 128;
    private static final int GRID_X = RIGHT_X;
    private static final int GRID_Y = 196;

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
    private ResourceLocation gridDimension;
    private int mapRange = 256;
    private int lastMapRange = 256;
    private int[][] mapColors = new int[GRID_SIZE][GRID_SIZE];
    private boolean mapDirty = true;
    private int lastMapCenterX;
    private int lastMapCenterZ;
    private int selectedPointIndex = -1;
    private int hoverPointIndex = -1;
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

        towerNameField = new EditBox(font, x + LEFT_X, y + 28, 120, 16, Component.translatable("screen.immersive_autopilot.tower_name"));
        towerNameField.setValue("default_tower");
        addRenderableWidget(towerNameField);

        rangeField = new EditBox(font, x + LEFT_X, y + 60, 60, 16, Component.translatable("screen.immersive_autopilot.scan_range"));
        rangeField.setValue("256");
        addRenderableWidget(rangeField);

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.refresh_list"),
                button -> refreshAircraftList())
                .bounds(x + LEFT_X + 70, y + 60, 62, 16).build());

        addRenderableWidget(Button.builder(Component.literal("<"),
                button -> prevPage())
                .bounds(x + LEFT_X, y + 86, 22, 16).build());

        addRenderableWidget(Button.builder(Component.literal(">"),
                button -> nextPage())
                .bounds(x + LEFT_X + 28, y + 86, 22, 16).build());

        routeNameField = new EditBox(font, x + RIGHT_X, y + 28, 120, 16, Component.translatable("screen.immersive_autopilot.route_name"));
        routeNameField.setValue(activeRoute.getName().isEmpty() ? "default" : activeRoute.getName());
        addRenderableWidget(routeNameField);

        speedField = new EditBox(font, x + RIGHT_X, y + 60, 50, 16, Component.translatable("screen.immersive_autopilot.speed"));
        speedField.setValue("1.0");
        addRenderableWidget(speedField);

        holdField = new EditBox(font, x + RIGHT_X + 60, y + 60, 50, 16, Component.translatable("screen.immersive_autopilot.hold"));
        holdField.setValue("0");
        addRenderableWidget(holdField);

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.add_waypoint"),
                button -> addWaypoint())
                .bounds(x + RIGHT_X, y + 92, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.remove_waypoint"),
                button -> removeWaypoint())
                .bounds(x + RIGHT_X, y + 116, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.save_preset"),
                button -> savePreset())
                .bounds(x + RIGHT_X, y + 140, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.load_preset"),
                button -> loadPreset())
                .bounds(x + RIGHT_X, y + 164, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.send_route"),
                button -> sendRoute())
                .bounds(x + RIGHT_X, y + 176, 120, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.bind_selected"),
                button -> bindSelected())
                .bounds(x + LEFT_X, y + 220, 120, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.unbind"),
                button -> unbind())
                .bounds(x + LEFT_X, y + 246, 120, 20).build());

        targetModeButton = Button.builder(Component.translatable("screen.immersive_autopilot.target_mode_bound"),
                button -> toggleTargetMode())
                .bounds(x + LEFT_X, y + 272, 120, 20).build();
        addRenderableWidget(targetModeButton);

        autoRequestField = new EditBox(font, x + LEFT_X, y + 334, 296, 16, Component.translatable("screen.immersive_autopilot.auto_request"));
        autoRequestField.setValue("Auto request from {tower}");
        addRenderableWidget(autoRequestField);

        enterField = new EditBox(font, x + LEFT_X, y + 356, 296, 16, Component.translatable("screen.immersive_autopilot.enter_text"));
        enterField.setValue("Entering {tower}");
        addRenderableWidget(enterField);

        exitField = new EditBox(font, x + LEFT_X, y + 378, 296, 16, Component.translatable("screen.immersive_autopilot.exit_text"));
        exitField.setValue("Leaving {tower}");
        addRenderableWidget(exitField);

        addRenderableWidget(Button.builder(Component.translatable("screen.immersive_autopilot.apply_config"),
                button -> applyConfig())
                .bounds(x + LEFT_X, y + 300, 120, 18).build());

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            gridCenterX = menu.getPos().getX();
            gridCenterZ = menu.getPos().getZ();
            gridDimension = player.level().dimension().location();
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
        RouteProgram program = new RouteProgram(name, new ArrayList<>(activeRoute.getWaypoints()));
        for (com.immersiveautopilot.route.RouteLink link : activeRoute.getLinks()) {
            program.addLink(link.from(), link.to());
        }
        return program;
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
            if (state.getScanRange() != mapRange) {
                mapRange = Math.max(1, state.getScanRange());
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
        graphics.drawString(font, title, leftPos + 8, topPos + 6, 0xFFFFFFFF, true);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.tower_name"), leftPos + LEFT_X, topPos + 12, 0xFFFFFFFF, true);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.scan_range"), leftPos + LEFT_X, topPos + 46, 0xFFFFFFFF, true);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.route_name"), leftPos + RIGHT_X, topPos + 12, 0xFFFFFFFF, true);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.speed"), leftPos + RIGHT_X, topPos + 46, 0xFFFFFFFF, true);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.hold"), leftPos + RIGHT_X + 60, topPos + 46, 0xFFFFFFFF, true);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.aircraft"), leftPos + LEFT_X, topPos + 104, 0xFFFFFFFF, true);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.waypoints"), leftPos + RIGHT_X, topPos + 104, 0xFFFFFFFF, true);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.bound_aircraft"), leftPos + LEFT_X, topPos + 202, 0xFFFFFFFF, true);
        String boundText = boundName == null || boundName.isBlank() ? Component.translatable("screen.immersive_autopilot.no_bound_aircraft").getString() : boundName;
        graphics.drawString(font, boundText, leftPos + LEFT_X, topPos + 214, 0xFFFFFFFF, true);

        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.auto_request"), leftPos + LEFT_X, topPos + 322, 0xFFFFFFFF, true);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.enter_text"), leftPos + LEFT_X, topPos + 346, 0xFFFFFFFF, true);
        graphics.drawString(font, Component.translatable("screen.immersive_autopilot.exit_text"), leftPos + LEFT_X, topPos + 370, 0xFFFFFFFF, true);

        drawAircraftList(graphics);
        drawRouteList(graphics);
        drawGrid(graphics);
        drawHoverCoords(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawHoverCoords(GuiGraphics graphics, int mouseX, int mouseY) {
        hoverPointIndex = findPointIndexAt(mouseX, mouseY);
        if (hoverPointIndex >= 0) {
            RouteWaypoint wp = activeRoute.getWaypoints().get(hoverPointIndex);
            String text = wp.getPos().getX() + ", " + wp.getPos().getY() + ", " + wp.getPos().getZ();
            graphics.drawString(font, text, mouseX + 8, mouseY + 8, 0xFFFFFFFF, true);
        }
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
        int listY = topPos + 122;

        int start = page * ROWS;
        for (int i = 0; i < ROWS; i++) {
            int index = start + i;
            int y = listY + i * ROW_HEIGHT;
            if (index >= aircraft.size()) {
                continue;
            }
            AircraftSnapshot snapshot = aircraft.get(index);
            int color = index == selectedIndex ? 0xFFFFC04D : 0xFFFFFFFF;
            String line = snapshot.getName() + " [" + (int) snapshot.getDistance() + "m]";
            graphics.drawString(font, line, listX, y, color, true);
        }
    }

    private void drawRouteList(GuiGraphics graphics) {
        int listX = leftPos + RIGHT_X;
        int listY = topPos + 122;
        List<RouteWaypoint> points = activeRoute.getWaypoints();
        int max = Math.min(points.size(), 4);
        for (int i = 0; i < max; i++) {
            RouteWaypoint wp = points.get(i);
            String line = (i + 1) + ": " + wp.getPos().getX() + "," + wp.getPos().getZ();
            graphics.drawString(font, line, listX, listY + i * ROW_HEIGHT, 0xFFFFFFFF, true);
        }
    }

    private void drawGrid(GuiGraphics graphics) {
        int x0 = leftPos + GRID_X;
        int y0 = topPos + GRID_Y;
        int x1 = x0 + GRID_SIZE;
        int y1 = y0 + GRID_SIZE;

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Level level = player.level();
        updateMapCache(level);

        for (int dz = 0; dz < GRID_SIZE; dz++) {
            for (int dx = 0; dx < GRID_SIZE; dx++) {
                int color = mapColors[dx][dz];
                graphics.fill(x0 + dx, y0 + dz, x0 + dx + 1, y0 + dz + 1, color);
            }
        }

        for (int i = 0; i <= GRID_SIZE; i += 16) {
            graphics.hLine(x0, x1, y0 + i, 0x5522262B);
            graphics.vLine(x0 + i, y0, y1, 0x5522262B);
        }
        graphics.hLine(x0, x1, y0 + GRID_SIZE / 2, 0xFF2F343A);
        graphics.vLine(x0 + GRID_SIZE / 2, y0, y1, 0xFF2F343A);
        List<RouteWaypoint> points = activeRoute.getWaypoints();
        for (com.immersiveautopilot.route.RouteLink link : activeRoute.getLinks()) {
            if (link.from() < 0 || link.to() < 0 || link.from() >= points.size() || link.to() >= points.size()) {
                continue;
            }
            RouteWaypoint from = points.get(link.from());
            RouteWaypoint to = points.get(link.to());
            if (!from.getDimension().equals(level.dimension().location()) || !to.getDimension().equals(level.dimension().location())) {
                continue;
            }
            double blocksPerPixel = (mapRange * 2.0) / GRID_SIZE;
            int fx = x0 + (int) Math.round(GRID_SIZE / 2.0 + (from.getPos().getX() - gridCenterX) / blocksPerPixel);
            int fz = y0 + (int) Math.round(GRID_SIZE / 2.0 + (from.getPos().getZ() - gridCenterZ) / blocksPerPixel);
            int tx = x0 + (int) Math.round(GRID_SIZE / 2.0 + (to.getPos().getX() - gridCenterX) / blocksPerPixel);
            int tz = y0 + (int) Math.round(GRID_SIZE / 2.0 + (to.getPos().getZ() - gridCenterZ) / blocksPerPixel);
            drawArrow(graphics, fx, fz, tx, tz, 0xFF4FC3F7);
        }

        for (int i = 0; i < points.size(); i++) {
            RouteWaypoint wp = points.get(i);
            if (!wp.getDimension().equals(level.dimension().location())) {
                continue;
            }
            double blocksPerPixel = (mapRange * 2.0) / GRID_SIZE;
            int px = x0 + (int) Math.round(GRID_SIZE / 2.0 + (wp.getPos().getX() - gridCenterX) / blocksPerPixel);
            int pz = y0 + (int) Math.round(GRID_SIZE / 2.0 + (wp.getPos().getZ() - gridCenterZ) / blocksPerPixel);
            if (px < x0 || px >= x1 || pz < y0 || pz >= y1) {
                continue;
            }
            int pointColor = i == selectedPointIndex ? 0xFFFF4040 : 0xFF4FC3F7;
            graphics.fill(px - 1, pz - 1, px + 2, pz + 2, pointColor);
        }
    }

    private void updateMapCache(Level level) {
        ResourceLocation dim = level.dimension().location();
        if (gridDimension == null || !gridDimension.equals(dim)) {
            gridDimension = dim;
            mapDirty = true;
        }
        if (mapDirty || gridCenterX != lastMapCenterX || gridCenterZ != lastMapCenterZ || mapRange != lastMapRange) {
            lastMapCenterX = gridCenterX;
            lastMapCenterZ = gridCenterZ;
            lastMapRange = mapRange;
            double blocksPerPixel = (mapRange * 2.0) / GRID_SIZE;
            for (int dz = 0; dz < GRID_SIZE; dz++) {
                int worldZ = gridCenterZ + (int) Math.round((dz - GRID_SIZE / 2.0) * blocksPerPixel);
                for (int dx = 0; dx < GRID_SIZE; dx++) {
                    int worldX = gridCenterX + (int) Math.round((dx - GRID_SIZE / 2.0) * blocksPerPixel);
                    net.minecraft.core.BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new net.minecraft.core.BlockPos(worldX, 0, worldZ));
                    BlockState state = level.getBlockState(surface);
                    int color = state.getMapColor(level, surface).col;
                    mapColors[dx][dz] = 0xFF000000 | color;
                }
            }
            mapDirty = false;
        }
    }

    private void drawArrow(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        drawLine(graphics, x0, y0, x1, y1, color);
        double angle = Math.atan2(y1 - y0, x1 - x0);
        int len = 4;
        int leftX = (int) (x1 - len * Math.cos(angle - Math.PI / 6));
        int leftY = (int) (y1 - len * Math.sin(angle - Math.PI / 6));
        int rightX = (int) (x1 - len * Math.cos(angle + Math.PI / 6));
        int rightY = (int) (y1 - len * Math.sin(angle + Math.PI / 6));
        drawLine(graphics, x1, y1, leftX, leftY, color);
        drawLine(graphics, x1, y1, rightX, rightY, color);
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
                int hit = findPointIndexAt(mouseX, mouseY);
                if (hit >= 0) {
                    activeRoute.removeWaypointAt(hit);
                    if (selectedPointIndex == hit) {
                        selectedPointIndex = -1;
                    }
                } else {
                    addWaypointFromGrid(mouseX, mouseY);
                }
                return true;
            }
            if (button == 1) {
                int hit = findPointIndexAt(mouseX, mouseY);
                if (hit >= 0) {
                    if (selectedPointIndex == -1) {
                        selectedPointIndex = hit;
                    } else if (selectedPointIndex == hit) {
                        selectedPointIndex = -1;
                    } else {
                        activeRoute.addLink(selectedPointIndex, hit);
                        selectedPointIndex = -1;
                    }
                } else {
                    startDrag(mouseX, mouseY);
                }
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
        double blocksPerPixel = (mapRange * 2.0) / GRID_SIZE;
        double gridX = mouseX - x0 - GRID_SIZE / 2.0;
        double gridZ = mouseY - y0 - GRID_SIZE / 2.0;
        int worldX = gridCenterX + (int) Math.round(gridX * blocksPerPixel);
        int worldZ = gridCenterZ + (int) Math.round(gridZ * blocksPerPixel);
        float speed = parseFloat(speedField.getValue(), 1.0f);
        int hold = parseInt(holdField.getValue(), 0);
        RouteProgram program = buildProgramFromUi();
        program.addWaypoint(new RouteWaypoint(new net.minecraft.core.BlockPos(worldX, player.blockPosition().getY(), worldZ),
                player.level().dimension().location(), speed, hold));
        activeRoute = program;
        selectedPointIndex = activeRoute.getWaypoints().size() - 1;
    }

    private boolean isInsideGrid(double mouseX, double mouseY) {
        int x0 = leftPos + GRID_X;
        int y0 = topPos + GRID_Y;
        return mouseX >= x0 && mouseX <= x0 + GRID_SIZE && mouseY >= y0 && mouseY <= y0 + GRID_SIZE;
    }

    private int findPointIndexAt(double mouseX, double mouseY) {
        if (!isInsideGrid(mouseX, mouseY)) {
            return -1;
        }
        int x0 = leftPos + GRID_X;
        int y0 = topPos + GRID_Y;
        List<RouteWaypoint> points = activeRoute.getWaypoints();
        double blocksPerPixel = (mapRange * 2.0) / GRID_SIZE;
        for (int i = 0; i < points.size(); i++) {
            RouteWaypoint wp = points.get(i);
            if (!wp.getDimension().equals(Minecraft.getInstance().level.dimension().location())) {
                continue;
            }
            int px = x0 + (int) Math.round(GRID_SIZE / 2.0 + (wp.getPos().getX() - gridCenterX) / blocksPerPixel);
            int pz = y0 + (int) Math.round(GRID_SIZE / 2.0 + (wp.getPos().getZ() - gridCenterZ) / blocksPerPixel);
            double dist = Math.hypot(mouseX - px, mouseY - pz);
            if (dist <= 4.0) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInsideAircraftList(double mouseX, double mouseY) {
        int listX = leftPos + LEFT_X;
        int listY = topPos + 122;
        int listWidth = 120;
        int listHeight = ROWS * ROW_HEIGHT;
        return mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight;
    }

    private int getAircraftIndex(double mouseX, double mouseY) {
        int listY = topPos + 122;
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
