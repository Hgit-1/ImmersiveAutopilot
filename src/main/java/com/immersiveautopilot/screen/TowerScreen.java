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
import com.immersiveautopilot.network.C2SSendRouteToAircraft;
import com.immersiveautopilot.network.C2SUnbindAircraft;
import com.immersiveautopilot.route.RouteProgram;
import com.immersiveautopilot.route.RouteWaypoint;
import immersive_aircraft.cobalt.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TowerScreen extends AbstractContainerScreen<TowerMenu> {
    private enum PageMode {
        BASE,
        ROUTE
    }

    private static final int ROWS = 4;
    private static final int ROW_HEIGHT = 14;
    private static final int WAYPOINT_ROWS = 6;

    private int gridSize = 80;
    private static final int SCROLLBAR_WIDTH = 10;
    private static final int SCROLLBAR_PADDING = 4;
    private static final int MIN_PANEL_WIDTH = 240;
    private static final int MIN_PANEL_HEIGHT = 260;
    private static final int BASE_LEFT_X = 12;
    private static final int BASE_RIGHT_X = 170;
    private static final int BASE_GRID_Y = 200;

    private EditBox towerNameField;
    private EditBox rangeField;
    private EditBox routeNameField;
    private EditBox autoRequestField;
    private EditBox enterField;
    private EditBox exitField;
    private EditBox waypointYField;

    private Button targetModeButton;
    private Button tabBaseButton;
    private Button tabRouteButton;
    private Button applyWaypointButton;
    private Button deleteWaypointButton;
    private Button savePresetButton;
    private Button loadPresetButton;
    private Button sendRouteButton;

    private List<AircraftSnapshot> aircraft = new ArrayList<>();
    private int selectedIndex = -1;
    private int aircraftScroll = 0;
    private int waypointScroll = 0;

    private UUID boundUuid;
    private String boundName = "";

    private RouteProgram activeRoute = new RouteProgram("default");

    private int gridCenterX;
    private int gridCenterZ;
    private ResourceLocation gridDimension;
    private int mapRange = 256;
    private int lastMapRange = 256;
    private int mapBuildRow = 0;
    private int[][] mapColors = new int[gridSize][gridSize];
    private boolean mapDirty = true;
    private int lastMapCenterX;
    private int lastMapCenterZ;
    private int selectedPointIndex = -1;
    private int lastSelectedPointIndex = -1;
    private int hoverPointIndex = -1;
    private boolean draggingGrid = false;
    private double dragStartX;
    private double dragStartY;
    private int dragStartCenterX;
    private int dragStartCenterZ;

    private boolean targetAllInRange = false;
    private boolean localRouteDirty = false;
    private PageMode pageMode = PageMode.BASE;
    private final List<AbstractWidget> baseWidgets = new ArrayList<>();
    private final List<AbstractWidget> routeWidgets = new ArrayList<>();
    private final List<WidgetSlot> baseSlots = new ArrayList<>();
    private final List<WidgetSlot> routeSlots = new ArrayList<>();
    private int baseScrollOffset = 0;
    private int routeScrollOffset = 0;
    private int baseContentHeight = 0;
    private int routeContentHeight = 0;
    private boolean draggingScroll = false;
    private int scrollDragStartY = 0;
    private int dragStartScroll = 0;
    private long lastScrollUpdateMs = 0L;
    private static final long SCROLL_UPDATE_INTERVAL_MS = 80L;
    private int leftX = BASE_LEFT_X;
    private int rightX = BASE_RIGHT_X;
    private int gridX = BASE_RIGHT_X;
    private int gridY = BASE_GRID_Y;
    private int lastAppliedScrollOffset = Integer.MIN_VALUE;
    private int rightColumnWidth = 120;
    private int wideFieldWidth = 296;
    private int leftListWidth = 120;
    private int rightListWidth = 140;

    public TowerScreen(TowerMenu menu, net.minecraft.world.entity.player.Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = 400;
    }

    private static final class WidgetSlot {
        private final AbstractWidget widget;
        private final int baseX;
        private final int baseY;
        private final int height;

        private WidgetSlot(AbstractWidget widget, int baseX, int baseY, int height) {
            this.widget = widget;
            this.baseX = baseX;
            this.baseY = baseY;
            this.height = height;
        }
    }

    @Override
    protected void init() {
        this.imageWidth = Math.max(MIN_PANEL_WIDTH, Math.min(320, this.width - 16));
        this.imageHeight = Math.max(MIN_PANEL_HEIGHT, Math.min(400, this.height - 16));
        super.init();
        int x = leftPos;
        int y = topPos;
        updateLayoutMetrics();

        tabBaseButton = Button.builder(Component.translatable("screen.immersive_autopilot.tab_base"),
                button -> setPageMode(PageMode.BASE))
                .bounds(x + leftX, y + 6, 70, 16).build();
        addRenderableWidget(tabBaseButton);

        tabRouteButton = Button.builder(Component.translatable("screen.immersive_autopilot.tab_routes"),
                button -> setPageMode(PageMode.ROUTE))
                .bounds(x + leftX + 74, y + 6, 70, 16).build();
        addRenderableWidget(tabRouteButton);

        towerNameField = new EditBox(font, x + leftX, y + 32, leftListWidth, 16, Component.translatable("screen.immersive_autopilot.tower_name"));
        towerNameField.setValue("default_tower");
        addBaseWidget(towerNameField);

        rangeField = new EditBox(font, x + leftX, y + 64, 60, 16, Component.translatable("screen.immersive_autopilot.scan_range"));
        rangeField.setValue("64");
        rangeField.setEditable(false);
        rangeField.active = false;
        addBaseWidget(rangeField);

        addBaseWidget(Button.builder(Component.translatable("screen.immersive_autopilot.refresh_list"),
                button -> refreshAircraftList())
                .bounds(x + leftX + 70, y + 64, leftListWidth - 70, 16).build());

        addBaseWidget(Button.builder(Component.literal("<"),
                button -> prevPage())
                .bounds(x + leftX, y + 90, 22, 16).build());

        addBaseWidget(Button.builder(Component.literal(">"),
                button -> nextPage())
                .bounds(x + leftX + 28, y + 90, 22, 16).build());

        routeNameField = new EditBox(font, x + rightX, y + 32, rightColumnWidth, 16, Component.translatable("screen.immersive_autopilot.route_name"));
        routeNameField.setValue(activeRoute.getName().isEmpty() ? "default" : activeRoute.getName());
        addRouteWidget(routeNameField);

        savePresetButton = Button.builder(Component.translatable("screen.immersive_autopilot.save_preset"),
                button -> savePreset())
                .bounds(x + rightX, y + 54, rightColumnWidth, 18).build();
        addRouteWidget(savePresetButton);

        loadPresetButton = Button.builder(Component.translatable("screen.immersive_autopilot.load_preset"),
                button -> loadPreset())
                .bounds(x + rightX, y + 76, rightColumnWidth, 18).build();
        addRouteWidget(loadPresetButton);

        sendRouteButton = Button.builder(Component.translatable("screen.immersive_autopilot.send_route"),
                button -> sendRoute())
                .bounds(x + rightX, y + 98, rightColumnWidth, 18).build();
        addRouteWidget(sendRouteButton);

        addBaseWidget(Button.builder(Component.translatable("screen.immersive_autopilot.bind_selected"),
                button -> bindSelected())
                .bounds(x + leftX, y + 214, leftListWidth, 20).build());

        addBaseWidget(Button.builder(Component.translatable("screen.immersive_autopilot.unbind"),
                button -> unbind())
                .bounds(x + leftX, y + 240, leftListWidth, 20).build());

        targetModeButton = Button.builder(Component.translatable("screen.immersive_autopilot.target_mode_bound"),
                button -> toggleTargetMode())
                .bounds(x + leftX, y + 266, leftListWidth, 20).build();
        addBaseWidget(targetModeButton);

        autoRequestField = new EditBox(font, x + rightX, y + 32, rightColumnWidth, 16, Component.translatable("screen.immersive_autopilot.auto_request"));
        autoRequestField.setValue("Auto request from {tower}");
        addBaseWidget(autoRequestField);

        enterField = new EditBox(font, x + rightX, y + 54, rightColumnWidth, 16, Component.translatable("screen.immersive_autopilot.enter_text"));
        enterField.setValue("Entering {tower}");
        addBaseWidget(enterField);

        exitField = new EditBox(font, x + rightX, y + 76, rightColumnWidth, 16, Component.translatable("screen.immersive_autopilot.exit_text"));
        exitField.setValue("Leaving {tower}");
        addBaseWidget(exitField);

        addBaseWidget(Button.builder(Component.translatable("screen.immersive_autopilot.apply_config"),
                button -> applyConfig())
                .bounds(x + leftX, y + 288, leftListWidth, 18).build());

        int waypointBaseY = y + gridY + gridSize + 12;
        waypointYField = new EditBox(font, x + leftX, waypointBaseY, leftListWidth, 16, Component.translatable("screen.immersive_autopilot.waypoint_y"));
        waypointYField.setValue("0");
        addRouteWidget(waypointYField);

        applyWaypointButton = Button.builder(Component.translatable("screen.immersive_autopilot.apply_waypoint"),
                button -> applyWaypointEdit())
                .bounds(x + leftX, waypointBaseY + 22, leftListWidth, 18).build();
        addRouteWidget(applyWaypointButton);

        deleteWaypointButton = Button.builder(Component.translatable("screen.immersive_autopilot.delete_waypoint"),
                button -> deleteSelectedWaypoint())
                .bounds(x + leftX, waypointBaseY + 44, leftListWidth, 18).build();
        addRouteWidget(deleteWaypointButton);

        setPageMode(PageMode.BASE);

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            gridCenterX = menu.getPos().getX();
            gridCenterZ = menu.getPos().getZ();
            gridDimension = player.level().dimension().location();
        }
        mapDirty = true;
        mapBuildRow = 0;

        NetworkHandler.sendToServer(new C2SRequestTowerState(menu.getPos()));
        refreshAircraftList();
        updateContentHeights();
        applyScrollToWidgets();
    }

    private void refreshAircraftList() {
        NetworkHandler.sendToServer(new C2SRequestAircraftList(menu.getPos()));
    }

    private void addBaseWidget(AbstractWidget widget) {
        addRenderableWidget(widget);
        baseWidgets.add(widget);
        baseSlots.add(new WidgetSlot(widget, getWidgetX(widget), getWidgetY(widget), getWidgetHeight(widget)));
    }

    private void addRouteWidget(AbstractWidget widget) {
        addRenderableWidget(widget);
        routeWidgets.add(widget);
        routeSlots.add(new WidgetSlot(widget, getWidgetX(widget), getWidgetY(widget), getWidgetHeight(widget)));
    }

    private void setPageMode(PageMode mode) {
        this.pageMode = mode;
        boolean base = mode == PageMode.BASE;
        tabBaseButton.active = !base;
        tabRouteButton.active = base;
        for (AbstractWidget widget : baseWidgets) {
            widget.visible = base;
            widget.active = base;
        }
        for (AbstractWidget widget : routeWidgets) {
            widget.visible = !base;
            widget.active = !base;
        }
        updateWaypointControls();
        mapDirty = true;
        mapBuildRow = 0;
        updateContentHeights();
    }

    private void updateLayoutMetrics() {
        leftX = BASE_LEFT_X;
        int contentWidth = imageWidth - 16 - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
        int desiredSplit = Math.max(140, contentWidth / 2);
        int maxRight = leftX + contentWidth - 110;
        rightX = Math.min(leftX + desiredSplit, maxRight);
        if (rightX < leftX + 120) {
            rightX = leftX + 120;
        }

        rightColumnWidth = Math.max(110, contentWidth - rightX - 8);
        wideFieldWidth = Math.max(160, rightColumnWidth);
        leftListWidth = Math.max(110, rightX - leftX - 12);
        rightListWidth = Math.max(110, rightColumnWidth);

        ensureMapSize(leftListWidth);
        gridX = leftX;
        gridY = BASE_GRID_Y;
    }

    private int getContentX0() {
        return leftPos + 8;
    }

    private int getContentY0() {
        return topPos + 4;
    }

    private int getContentX1() {
        return leftPos + imageWidth - 8 - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
    }

    private int getContentY1() {
        return topPos + imageHeight - 8;
    }

    private int getViewportHeight() {
        return getContentY1() - getContentY0();
    }

    private void ensureMapSize(int newSize) {
        int resolved = Math.max(1, newSize);
        if (resolved == gridSize) {
            return;
        }
        gridSize = resolved;
        mapColors = new int[gridSize][gridSize];
        mapDirty = true;
        mapBuildRow = 0;
    }

    private void drawFieldLabel(GuiGraphics graphics, Component label, AbstractWidget field) {
        if (field == null) {
            return;
        }
        int x = field.getX();
        int y = field.getY() - (font.lineHeight + 2);
        graphics.drawString(font, label, x, y, 0xFFFFFFFF, true);
    }

    private int getScrollOffset() {
        return pageMode == PageMode.BASE ? baseScrollOffset : routeScrollOffset;
    }

    private void setScrollOffset(int value) {
        int max = Math.max(0, getContentHeight() - getViewportHeight());
        int clamped = Math.max(0, Math.min(max, value));
        if (pageMode == PageMode.BASE) {
            baseScrollOffset = clamped;
        } else {
            routeScrollOffset = clamped;
        }
    }

    private int getContentHeight() {
        return pageMode == PageMode.BASE ? baseContentHeight : routeContentHeight;
    }

    private void updateContentHeights() {
        int contentTop = getContentY0();
        int baseMax = contentTop;
        for (WidgetSlot slot : baseSlots) {
            baseMax = Math.max(baseMax, slot.baseY + slot.height);
        }
        baseContentHeight = Math.max(1, baseMax - contentTop + 8);

        int routeMax = contentTop;
        for (WidgetSlot slot : routeSlots) {
            routeMax = Math.max(routeMax, slot.baseY + slot.height);
        }
        int gridBottom = topPos + gridY + gridSize;
        routeMax = Math.max(routeMax, gridBottom + 8);
        routeContentHeight = Math.max(1, routeMax - contentTop + 8);
    }

    private void applyScrollToWidgets() {
        int offset = getScrollOffset();
        if (pageMode == PageMode.BASE) {
            for (WidgetSlot slot : baseSlots) {
                setWidgetPosition(slot.widget, slot.baseX, slot.baseY - offset);
            }
        } else {
            for (WidgetSlot slot : routeSlots) {
                setWidgetPosition(slot.widget, slot.baseX, slot.baseY - offset);
            }
        }
    }

    private void applyScrollToWidgetsIfNeeded() {
        long now = Util.getMillis();
        int offset = getScrollOffset();
        if (offset != lastAppliedScrollOffset) {
            lastAppliedScrollOffset = offset;
            applyScrollToWidgets();
            return;
        }
        if (now - lastScrollUpdateMs < SCROLL_UPDATE_INTERVAL_MS) {
            return;
        }
        lastScrollUpdateMs = now;
        applyScrollToWidgets();
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

    private void updateWaypointControls() {
        boolean active = selectedPointIndex >= 0 && selectedPointIndex < activeRoute.getWaypoints().size();
        applyWaypointButton.active = active;
        deleteWaypointButton.active = active;
        waypointYField.active = active;
        if (active) {
            RouteWaypoint wp = activeRoute.getWaypoints().get(selectedPointIndex);
            if (!waypointYField.isFocused() || selectedPointIndex != lastSelectedPointIndex) {
                waypointYField.setValue(Integer.toString(wp.getPos().getY()));
            }
        }
        lastSelectedPointIndex = selectedPointIndex;
    }

    private void applyWaypointEdit() {
        if (selectedPointIndex < 0 || selectedPointIndex >= activeRoute.getWaypoints().size()) {
            return;
        }
        RouteWaypoint current = activeRoute.getWaypoints().get(selectedPointIndex);
        int y = parseInt(waypointYField.getValue(), current.getPos().getY());
        float speed = current.getSpeed();
        int hold = current.getHoldSeconds();
        List<RouteWaypoint> newPoints = new ArrayList<>(activeRoute.getWaypoints());
        newPoints.set(selectedPointIndex, new RouteWaypoint(
                new net.minecraft.core.BlockPos(current.getPos().getX(), y, current.getPos().getZ()),
                current.getDimension(), speed, hold));
        RouteProgram program = new RouteProgram(activeRoute.getName(), newPoints);
        for (com.immersiveautopilot.route.RouteLink link : activeRoute.getLinks()) {
            program.addLink(link.from(), link.to());
        }
        activeRoute = program;
        localRouteDirty = true;
    }

    private void deleteSelectedWaypoint() {
        if (selectedPointIndex < 0 || selectedPointIndex >= activeRoute.getWaypoints().size()) {
            return;
        }
        RouteProgram program = buildProgramFromUi();
        program.removeWaypointAt(selectedPointIndex);
        activeRoute = program;
        selectedPointIndex = -1;
        localRouteDirty = true;
        int maxStart = Math.max(0, activeRoute.getWaypoints().size() - WAYPOINT_ROWS);
        waypointScroll = Math.min(waypointScroll, maxStart);
        updateWaypointControls();
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


    private void savePreset() {
        String name = routeNameField.getValue();
        if (name == null || name.isBlank()) {
            return;
        }
        RouteProgram program = buildProgramFromUi();
        NetworkHandler.sendToServer(new C2SSavePreset(menu.getPos(), name, program));
        localRouteDirty = false;
    }

    private void loadPreset() {
        String name = routeNameField.getValue();
        if (name == null || name.isBlank()) {
            return;
        }
        NetworkHandler.sendToServer(new C2SLoadPreset(menu.getPos(), name));
        localRouteDirty = false;
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
        aircraftScroll = Math.max(0, aircraftScroll - ROWS);
    }

    private void nextPage() {
        int maxStart = Math.max(0, aircraft.size() - ROWS);
        aircraftScroll = Math.min(maxStart, aircraftScroll + ROWS);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        applyScrollToWidgetsIfNeeded();
        TowerState state = ClientCache.getTowerState(menu.getPos());
        if (state != null) {
            boundUuid = state.getBoundAircraft();
            boundName = state.getBoundName();
            targetAllInRange = state.isTargetAllInRange();
            updateTargetButtonLabel();
            targetModeButton.visible = state.isPowered() && pageMode == PageMode.BASE;
            targetModeButton.active = state.isPowered() && pageMode == PageMode.BASE;
            if (!towerNameField.isFocused()) {
                towerNameField.setValue(state.getTowerName());
            }
            rangeField.setValue(Integer.toString(state.getScanRange()));
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
                if (shouldUpdate && !localRouteDirty) {
                    activeRoute = state.getActiveRoute();
                    if (!routeNameField.isFocused()) {
                        routeNameField.setValue(activeRoute.getName());
                    }
                }
            }
            if (selectedPointIndex >= activeRoute.getWaypoints().size()) {
                selectedPointIndex = -1;
            }
            updateWaypointControls();
        }

        List<AircraftSnapshot> newList = ClientCache.getAircraftList(menu.getPos());
        if (newList != aircraft) {
            aircraft = newList;
            if (selectedIndex >= aircraft.size()) {
                selectedIndex = -1;
            }
        }

        Player player = Minecraft.getInstance().player;
        if (player != null && pageMode == PageMode.ROUTE) {
            updateMapCache(player.level());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        applyScrollToWidgetsIfNeeded();
        // Avoid background blur from other UI mods.
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF0F1114);

        int contentX0 = getContentX0();
        int contentY0 = getContentY0();
        int contentX1 = getContentX1();
        int contentY1 = getContentY1();
        graphics.drawString(font, title, leftPos + 8, topPos + 6, 0xFFFFFFFF, true);
        graphics.enableScissor(contentX0, contentY0, contentX1, contentY1);

        if (pageMode == PageMode.BASE) {
            drawAircraftList(graphics);
        } else {
            drawGrid(graphics, partialTick);
            drawRouteList(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        int offset = getScrollOffset();
        if (pageMode == PageMode.BASE) {
            drawFieldLabel(graphics, Component.translatable("screen.immersive_autopilot.tower_name"), towerNameField);
            drawFieldLabel(graphics, Component.translatable("screen.immersive_autopilot.scan_range"), rangeField);

            int listY = topPos + 126 - offset;
            graphics.drawString(font, Component.translatable("screen.immersive_autopilot.aircraft"), leftPos + leftX, listY - (font.lineHeight + 2), 0xFFFFFFFF, true);

            int boundLabelY = topPos + 196 - offset;
            graphics.drawString(font, Component.translatable("screen.immersive_autopilot.bound_aircraft"), leftPos + leftX, boundLabelY, 0xFFFFFFFF, true);
            String boundText = boundName == null || boundName.isBlank() ? Component.translatable("screen.immersive_autopilot.no_bound_aircraft").getString() : boundName;
            graphics.drawString(font, boundText, leftPos + leftX, boundLabelY + font.lineHeight + 2, 0xFFFFFFFF, true);

            drawFieldLabel(graphics, Component.translatable("screen.immersive_autopilot.auto_request"), autoRequestField);
            drawFieldLabel(graphics, Component.translatable("screen.immersive_autopilot.enter_text"), enterField);
            drawFieldLabel(graphics, Component.translatable("screen.immersive_autopilot.exit_text"), exitField);
        } else {
            drawFieldLabel(graphics, Component.translatable("screen.immersive_autopilot.route_name"), routeNameField);
            int listY = topPos + 126 - offset;
            graphics.drawString(font, Component.translatable("screen.immersive_autopilot.waypoints"), leftPos + rightX, listY - (font.lineHeight + 2), 0xFFFFFFFF, true);
            drawFieldLabel(graphics, Component.translatable("screen.immersive_autopilot.waypoint_y"), waypointYField);
        }

        if (pageMode == PageMode.ROUTE) {
            drawHoverCoords(graphics, mouseX, mouseY);
        }
        graphics.disableScissor();
        drawScrollbar(graphics);
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

    private void drawScrollbar(GuiGraphics graphics) {
        int contentX0 = getContentX0();
        int contentY0 = getContentY0();
        int contentX1 = getContentX1();
        int contentY1 = getContentY1();
        int barX0 = contentX1 + SCROLLBAR_PADDING;
        int barX1 = barX0 + SCROLLBAR_WIDTH;
        int barY0 = contentY0;
        int barY1 = contentY1;

        graphics.fill(barX0, barY0, barX1, barY1, 0xFF1B1F26);

        int viewport = getViewportHeight();
        int content = getContentHeight();
        if (content <= viewport) {
            graphics.fill(barX0, barY0, barX1, barY1, 0xFF8FB3B3);
            return;
        }
        int maxScroll = content - viewport;
        int thumbHeight = Math.max(16, (int) Math.round(viewport * (viewport / (double) content)));
        int trackHeight = viewport - thumbHeight;
        int thumbY = barY0 + (int) Math.round(trackHeight * (getScrollOffset() / (double) maxScroll));
        graphics.fill(barX0, thumbY, barX1, thumbY + thumbHeight, 0xFFE5F5F5);
    }

    private boolean isInsideContent(double mouseX, double mouseY) {
        int x0 = getContentX0();
        int y0 = getContentY0();
        int x1 = getContentX1();
        int y1 = getContentY1();
        return mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1;
    }

    private boolean isInsideScrollbar(double mouseX, double mouseY) {
        int barX0 = getContentX1() + SCROLLBAR_PADDING;
        int barX1 = barX0 + SCROLLBAR_WIDTH;
        int barY0 = getContentY0();
        int barY1 = getContentY1();
        return mouseX >= barX0 && mouseX <= barX1 && mouseY >= barY0 && mouseY <= barY1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Background is drawn in render().
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Suppress default inventory labels.
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Avoid background blur from other UI mods.
    }

    private void drawAircraftList(GuiGraphics graphics) {
        int listX = leftPos + leftX;
        int listY = topPos + 126 - getScrollOffset();

        int start = aircraftScroll;
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
        int listX = leftPos + rightX;
        int listY = topPos + 126 - getScrollOffset();
        List<RouteWaypoint> points = activeRoute.getWaypoints();
        int max = Math.min(points.size() - waypointScroll, WAYPOINT_ROWS);
        for (int i = 0; i < max; i++) {
            int index = waypointScroll + i;
            RouteWaypoint wp = points.get(index);
            String line = (index + 1) + ": " + wp.getPos().getX() + "," + wp.getPos().getZ() + " Y=" + wp.getPos().getY();
            int color = index == selectedPointIndex ? 0xFFFFC04D : 0xFFFFFFFF;
            graphics.drawString(font, line, listX, listY + i * ROW_HEIGHT, color, true);
        }
    }

    private void drawGrid(GuiGraphics graphics, float partialTick) {
        int x0 = leftPos + gridX;
        int y0 = topPos + gridY - getScrollOffset();
        int x1 = x0 + gridSize;
        int y1 = y0 + gridSize;

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Level level = player.level();
        updateMapCache(level);

        for (int dz = 0; dz < gridSize; dz++) {
            for (int dx = 0; dx < gridSize; dx++) {
                int color = mapColors[dx][dz];
                graphics.fill(x0 + dx, y0 + dz, x0 + dx + 1, y0 + dz + 1, color);
            }
        }

        for (int i = 0; i <= gridSize; i += 16) {
            graphics.hLine(x0, x1, y0 + i, 0x5522262B);
            graphics.vLine(x0 + i, y0, y1, 0x5522262B);
        }
        graphics.hLine(x0, x1, y0 + gridSize / 2, 0xFF2F343A);
        graphics.vLine(x0 + gridSize / 2, y0, y1, 0xFF2F343A);
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
            double blocksPerPixel = (mapRange * 2.0) / gridSize;
            int fx = x0 + (int) Math.round(gridSize / 2.0 + (from.getPos().getX() - gridCenterX) / blocksPerPixel);
            int fz = y0 + (int) Math.round(gridSize / 2.0 + (from.getPos().getZ() - gridCenterZ) / blocksPerPixel);
            int tx = x0 + (int) Math.round(gridSize / 2.0 + (to.getPos().getX() - gridCenterX) / blocksPerPixel);
            int tz = y0 + (int) Math.round(gridSize / 2.0 + (to.getPos().getZ() - gridCenterZ) / blocksPerPixel);
            drawArrow(graphics, fx, fz, tx, tz, 0xFF4FC3F7);
        }

        for (int i = 0; i < points.size(); i++) {
            RouteWaypoint wp = points.get(i);
            if (!wp.getDimension().equals(level.dimension().location())) {
                continue;
            }
            double blocksPerPixel = (mapRange * 2.0) / gridSize;
            int px = x0 + (int) Math.round(gridSize / 2.0 + (wp.getPos().getX() - gridCenterX) / blocksPerPixel);
            int pz = y0 + (int) Math.round(gridSize / 2.0 + (wp.getPos().getZ() - gridCenterZ) / blocksPerPixel);
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
            mapBuildRow = 0;
        }
        if (gridCenterX != lastMapCenterX || gridCenterZ != lastMapCenterZ || mapRange != lastMapRange) {
            lastMapCenterX = gridCenterX;
            lastMapCenterZ = gridCenterZ;
            lastMapRange = mapRange;
            mapDirty = true;
            mapBuildRow = 0;
        }
        if (!mapDirty) {
            return;
        }
        double blocksPerPixel = (mapRange * 2.0) / gridSize;
        int rowsPerUpdate = 4;
        int endRow = Math.min(gridSize, mapBuildRow + rowsPerUpdate);
        int radarY = resolveRadarY(level);
        net.minecraft.core.BlockPos.MutableBlockPos mutable = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int dz = mapBuildRow; dz < endRow; dz++) {
            int worldZ = gridCenterZ + (int) Math.round((dz - gridSize / 2.0) * blocksPerPixel);
            for (int dx = 0; dx < gridSize; dx++) {
                int worldX = gridCenterX + (int) Math.round((dx - gridSize / 2.0) * blocksPerPixel);
                mutable.set(worldX, radarY, worldZ);
                if (!level.hasChunkAt(mutable)) {
                    mapColors[dx][dz] = 0xFF1B1F26;
                    continue;
                }
                net.minecraft.core.BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, mutable);
                int topY = surface.getY();
                if (topY < radarY) {
                    BlockState topState = level.getBlockState(surface);
                    int color = topState.getMapColor(level, surface).col;
                    mapColors[dx][dz] = 0xFF000000 | darken(color, radarY - topY);
                } else {
                    BlockState sliceState = level.getBlockState(mutable);
                    if (sliceState.isAir()) {
                        if (topY > radarY) {
                            mapColors[dx][dz] = 0xFF000000;
                        } else {
                            BlockState topState = level.getBlockState(surface);
                            int color = topState.getMapColor(level, surface).col;
                            mapColors[dx][dz] = 0xFF000000 | color;
                        }
                    } else {
                        int color = sliceState.getMapColor(level, mutable).col;
                        mapColors[dx][dz] = 0xFF000000 | color;
                    }
                }
            }
        }
        mapBuildRow = endRow;
        if (mapBuildRow >= gridSize) {
            mapDirty = false;
        }
    }

    private int resolveRadarY(Level level) {
        int y = menu.getPos().getY();
        int best = y;
        int bestBonus = -1;
        net.minecraft.core.BlockPos.MutableBlockPos mutable = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    mutable.set(menu.getPos().getX() + dx, menu.getPos().getY() + dy, menu.getPos().getZ() + dz);
                    if (level.getBlockEntity(mutable) instanceof com.immersiveautopilot.blockentity.RadarBlockEntity radar) {
                        int bonus = radar.getRangeBonus();
                        if (bonus > bestBonus) {
                            bestBonus = bonus;
                            best = mutable.getY();
                        }
                    }
                }
            }
        }
        return best;
    }

    private int darken(int color, int delta) {
        double factor = 1.0 - Math.min(0.6, delta / 96.0);
        int r = (int) Math.max(0, Math.min(255, ((color >> 16) & 0xFF) * factor));
        int g = (int) Math.max(0, Math.min(255, ((color >> 8) & 0xFF) * factor));
        int b = (int) Math.max(0, Math.min(255, (color & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
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
        if (button == 0 && isInsideScrollbar(mouseX, mouseY)) {
            draggingScroll = true;
            scrollDragStartY = (int) mouseY;
            dragStartScroll = getScrollOffset();
            return true;
        }
        if (pageMode == PageMode.BASE && button == 0 && isInsideAircraftList(mouseX, mouseY)) {
            int index = getAircraftIndex(mouseX, mouseY);
            if (index >= 0 && index < aircraft.size()) {
                selectedIndex = index;
                return true;
            }
        }
        if (pageMode == PageMode.ROUTE && button == 0 && isInsideWaypointList(mouseX, mouseY)) {
            int index = getWaypointIndex(mouseX, mouseY);
            if (index >= 0 && index < activeRoute.getWaypoints().size()) {
                selectedPointIndex = index;
                updateWaypointControls();
                return true;
            }
        }
        if (pageMode == PageMode.ROUTE && isInsideGrid(mouseX, mouseY)) {
            int hit = findPointIndexAt(mouseX, mouseY);
            if (button == 0) {
                if (hit >= 0) {
                    selectedPointIndex = hit;
                    updateWaypointControls();
                    return true;
                }
                return false;
            }
            if (button == 1) {
                if (hit >= 0) {
                    if (selectedPointIndex == -1) {
                        selectedPointIndex = hit;
                    } else if (selectedPointIndex == hit) {
                        selectedPointIndex = -1;
                    } else {
                        activeRoute.addLink(selectedPointIndex, hit);
                        selectedPointIndex = -1;
                        localRouteDirty = true;
                    }
                    updateWaypointControls();
                } else {
                    addWaypointFromGrid(mouseX, mouseY);
                }
                return true;
            }
            if (button == 2) {
                startDrag(mouseX, mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if ((button == 1 || button == 2) && draggingGrid) {
            draggingGrid = false;
            return true;
        }
        if (button == 0 && draggingScroll) {
            draggingScroll = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingGrid && (button == 1 || button == 2)) {
            int dx = (int) (mouseX - dragStartX);
            int dz = (int) (mouseY - dragStartY);
            gridCenterX = dragStartCenterX - dx;
            gridCenterZ = dragStartCenterZ - dz;
            return true;
        }
        if (draggingScroll && button == 0) {
            int viewport = getViewportHeight();
            int content = getContentHeight();
            if (content > viewport) {
                int maxScroll = content - viewport;
                int thumbHeight = Math.max(16, (int) Math.round(viewport * (viewport / (double) content)));
                int trackHeight = viewport - thumbHeight;
                int delta = (int) mouseY - scrollDragStartY;
                int scrollDelta = (int) Math.round(delta * (maxScroll / (double) Math.max(1, trackHeight)));
                setScrollOffset(dragStartScroll + scrollDelta);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (pageMode == PageMode.BASE && isInsideAircraftList(mouseX, mouseY)) {
            int maxStart = Math.max(0, aircraft.size() - ROWS);
            if (deltaY < 0) {
                aircraftScroll = Math.min(maxStart, aircraftScroll + 1);
            } else if (deltaY > 0) {
                aircraftScroll = Math.max(0, aircraftScroll - 1);
            }
            return true;
        }
        if (pageMode == PageMode.ROUTE && isInsideWaypointList(mouseX, mouseY)) {
            int maxStart = Math.max(0, activeRoute.getWaypoints().size() - WAYPOINT_ROWS);
            if (deltaY < 0) {
                waypointScroll = Math.min(maxStart, waypointScroll + 1);
            } else if (deltaY > 0) {
                waypointScroll = Math.max(0, waypointScroll - 1);
            }
            return true;
        }
        if (isInsideContent(mouseX, mouseY)) {
            int step = 18;
            if (deltaY < 0) {
                setScrollOffset(getScrollOffset() + step);
            } else if (deltaY > 0) {
                setScrollOffset(getScrollOffset() - step);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
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
        int x0 = leftPos + gridX;
        int y0 = topPos + gridY - getScrollOffset();
        double blocksPerPixel = (mapRange * 2.0) / gridSize;
        double gridX = mouseX - x0 - gridSize / 2.0;
        double gridZ = mouseY - y0 - gridSize / 2.0;
        int worldX = gridCenterX + (int) Math.round(gridX * blocksPerPixel);
        int worldZ = gridCenterZ + (int) Math.round(gridZ * blocksPerPixel);
        float speed = 1.0f;
        int hold = 0;
        int height = player.level().getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                new net.minecraft.core.BlockPos(worldX, 0, worldZ)).getY();
        RouteProgram program = buildProgramFromUi();
        program.addWaypoint(new RouteWaypoint(new net.minecraft.core.BlockPos(worldX, height, worldZ),
                player.level().dimension().location(), speed, hold));
        activeRoute = program;
        selectedPointIndex = activeRoute.getWaypoints().size() - 1;
        localRouteDirty = true;
        updateWaypointControls();
    }

    private boolean isInsideGrid(double mouseX, double mouseY) {
        int x0 = leftPos + gridX;
        int y0 = topPos + gridY - getScrollOffset();
        return mouseX >= x0 && mouseX <= x0 + gridSize && mouseY >= y0 && mouseY <= y0 + gridSize;
    }

    private int findPointIndexAt(double mouseX, double mouseY) {
        if (!isInsideGrid(mouseX, mouseY)) {
            return -1;
        }
        int x0 = leftPos + gridX;
        int y0 = topPos + gridY - getScrollOffset();
        List<RouteWaypoint> points = activeRoute.getWaypoints();
        double blocksPerPixel = (mapRange * 2.0) / gridSize;
        for (int i = 0; i < points.size(); i++) {
            RouteWaypoint wp = points.get(i);
            if (!wp.getDimension().equals(Minecraft.getInstance().level.dimension().location())) {
                continue;
            }
            int px = x0 + (int) Math.round(gridSize / 2.0 + (wp.getPos().getX() - gridCenterX) / blocksPerPixel);
            int pz = y0 + (int) Math.round(gridSize / 2.0 + (wp.getPos().getZ() - gridCenterZ) / blocksPerPixel);
            double dist = Math.hypot(mouseX - px, mouseY - pz);
            if (dist <= 4.0) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInsideAircraftList(double mouseX, double mouseY) {
        int listX = leftPos + leftX;
        int listY = topPos + 126 - getScrollOffset();
        int listWidth = leftListWidth;
        int listHeight = ROWS * ROW_HEIGHT;
        return mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight;
    }

    private int getAircraftIndex(double mouseX, double mouseY) {
        int listY = topPos + 126 - getScrollOffset();
        int row = (int) ((mouseY - listY) / ROW_HEIGHT);
        return aircraftScroll + row;
    }

    private int getWaypointIndex(double mouseX, double mouseY) {
        int listY = topPos + 126 - getScrollOffset();
        int row = (int) ((mouseY - listY) / ROW_HEIGHT);
        return waypointScroll + row;
    }

    private boolean isInsideWaypointList(double mouseX, double mouseY) {
        int listX = leftPos + rightX;
        int listY = topPos + 126 - getScrollOffset();
        int listWidth = rightListWidth;
        int listHeight = WAYPOINT_ROWS * ROW_HEIGHT;
        return mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int getWidgetX(AbstractWidget widget) {
        try {
            return (int) widget.getClass().getMethod("getX").invoke(widget);
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field field = widget.getClass().getDeclaredField("x");
            field.setAccessible(true);
            return field.getInt(widget);
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static int getWidgetY(AbstractWidget widget) {
        try {
            return (int) widget.getClass().getMethod("getY").invoke(widget);
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field field = widget.getClass().getDeclaredField("y");
            field.setAccessible(true);
            return field.getInt(widget);
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static int getWidgetHeight(AbstractWidget widget) {
        try {
            return (int) widget.getClass().getMethod("getHeight").invoke(widget);
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field field = widget.getClass().getDeclaredField("height");
            field.setAccessible(true);
            return field.getInt(widget);
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static void setWidgetPosition(AbstractWidget widget, int x, int y) {
        try {
            widget.getClass().getMethod("setPosition", int.class, int.class).invoke(widget, x, y);
            return;
        } catch (Exception ignored) {
        }
        try {
            widget.getClass().getMethod("setX", int.class).invoke(widget, x);
        } catch (Exception ignored) {
        }
        try {
            widget.getClass().getMethod("setY", int.class).invoke(widget, y);
        } catch (Exception ignored) {
        }
    }

}
