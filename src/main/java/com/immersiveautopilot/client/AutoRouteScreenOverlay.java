package com.immersiveautopilot.client;

import com.immersiveautopilot.ImmersiveAutopilot;
import immersive_aircraft.client.gui.VehicleScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, bus = EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AutoRouteScreenOverlay {
    private static final int PANEL_WIDTH = 120;
    private static final int LINE_HEIGHT = 18;
    private static final int LINE_COUNT = 5;
    private static final Map<Screen, Controller> CONTROLLERS = new WeakHashMap<>();

    private AutoRouteScreenOverlay() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof VehicleScreen screen)) {
            return;
        }
        Controller controller = new Controller(screen);
        controller.init(event);
        CONTROLLERS.put(screen, controller);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Controller controller = CONTROLLERS.get(event.getScreen());
        if (controller == null) {
            return;
        }
        controller.renderPanel(event.getGuiGraphics());
        controller.syncFromCache();
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        CONTROLLERS.remove(event.getScreen());
    }

    private static final class Controller {
        private final VehicleScreen screen;
        private final List<EditBox> fields = new ArrayList<>();
        private Button applyButton;
        private int panelX;
        private int panelY;
        private int panelHeight;

        private Controller(VehicleScreen screen) {
            this.screen = screen;
        }

        private void init(ScreenEvent.Init.Post event) {
            int left = screen.getX();
            int top = screen.getY();
            int imageWidth = getImageWidth(screen);
            panelX = left + imageWidth + 8;
            panelY = top + 6;
            panelHeight = 28 + LINE_COUNT * LINE_HEIGHT + 24;

            int fieldWidth = PANEL_WIDTH - 12;
            for (int i = 0; i < LINE_COUNT; i++) {
                int y = panelY + 20 + i * LINE_HEIGHT;
                EditBox field = new EditBox(screen.getFont(), panelX + 6, y, fieldWidth, 16, Component.literal(""));
                field.setMaxLength(64);
                event.addRenderableWidget(field);
                fields.add(field);
            }

            applyButton = Button.builder(Component.translatable("screen.immersive_autopilot.auto_routes_apply"),
                    button -> applyRoutes())
                .bounds(panelX + 6, panelY + panelHeight - 22, fieldWidth, 18)
                .build();
            event.addRenderableWidget(applyButton);
        }

        private void renderPanel(GuiGraphics graphics) {
            graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xFF101215);
            graphics.fill(panelX + 2, panelY + 2, panelX + PANEL_WIDTH - 2, panelY + panelHeight - 2, 0xFF1B1F26);
            graphics.drawString(screen.getFont(), Component.translatable("screen.immersive_autopilot.auto_routes_title"),
                    panelX + 6, panelY + 6, 0xFFFFFFFF, false);
        }

        private void syncFromCache() {
            List<AutoRouteClient.Entry> entries = AutoRouteClient.getRoutes(screen.getMenu().getVehicle().getId());
            for (int i = 0; i < fields.size(); i++) {
                EditBox field = fields.get(i);
                if (field.isFocused()) {
                    continue;
                }
                String value = "";
                if (i < entries.size()) {
                    AutoRouteClient.Entry entry = entries.get(i);
                    value = entry.label() == null || entry.label().isBlank()
                            ? entry.routeName()
                            : entry.routeName() + "|" + entry.label();
                }
                field.setValue(value);
            }
        }

        private void applyRoutes() {
            List<String> names = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (EditBox field : fields) {
                String line = field.getValue();
                if (line == null) {
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String name = trimmed;
                String label = "";
                int pipe = trimmed.indexOf('|');
                if (pipe >= 0) {
                    name = trimmed.substring(0, pipe).trim();
                    label = trimmed.substring(pipe + 1).trim();
                }
                if (name.isEmpty()) {
                    continue;
                }
                names.add(name);
                labels.add(label);
            }
            AutoRouteClient.setRoutes(screen.getMenu().getVehicle().getId(), buildEntries(names, labels));
        }

        private List<AutoRouteClient.Entry> buildEntries(List<String> names, List<String> labels) {
            List<AutoRouteClient.Entry> list = new ArrayList<>();
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                String label = i < labels.size() ? labels.get(i) : "";
                list.add(new AutoRouteClient.Entry(name, label));
            }
            return list;
        }

        private int getImageWidth(AbstractContainerScreen<?> screen) {
            try {
                Field field = AbstractContainerScreen.class.getDeclaredField("imageWidth");
                field.setAccessible(true);
                return field.getInt(screen);
            } catch (Exception ignored) {
                return 176;
            }
        }
    }
}
