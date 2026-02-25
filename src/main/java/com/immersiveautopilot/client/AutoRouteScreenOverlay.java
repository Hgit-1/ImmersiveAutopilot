package com.immersiveautopilot.client;

import com.immersiveautopilot.ImmersiveAutopilot;
import immersive_aircraft.client.gui.VehicleScreen;
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, value = Dist.CLIENT)
public final class AutoRouteScreenOverlay {
    private static final int PANEL_WIDTH = 120;
    private static final int LINE_HEIGHT = 18;
    private static final int LINE_COUNT = 5;
    private static final int CLEAR_BUTTON_WIDTH = 16;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("immersive_aircraft", "textures/gui/container/inventory.png");
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
        private final List<Button> clearButtons = new ArrayList<>();
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
            panelX = Math.max(4, left - PANEL_WIDTH - 8);
            panelY = top + 6;
            panelHeight = 28 + LINE_COUNT * LINE_HEIGHT + 24;

            int fieldWidth = PANEL_WIDTH - 12 - CLEAR_BUTTON_WIDTH - 4;
            for (int i = 0; i < LINE_COUNT; i++) {
                int y = panelY + 20 + i * LINE_HEIGHT;
                EditBox field = new EditBox(screen.getMinecraft().font, panelX + 6, y, fieldWidth, 16, Component.literal(""));
                field.setMaxLength(64);
                screen.addRenderableWidget(field);
                fields.add(field);

                Button clear = Button.builder(Component.literal("X"), button -> field.setValue(""))
                        .bounds(panelX + 6 + fieldWidth + 4, y, CLEAR_BUTTON_WIDTH, 16)
                        .build();
                screen.addRenderableWidget(clear);
                clearButtons.add(clear);
            }

            applyButton = Button.builder(Component.translatable("screen.immersive_autopilot.auto_routes_apply"),
                    button -> applyRoutes())
                .bounds(panelX + 6, panelY + panelHeight - 20, PANEL_WIDTH - 12, 18)
                .build();
            screen.addRenderableWidget(applyButton);

            AutoRouteClient.requestRoutes(screen.getMenu().getVehicle().getId());
        }

        private void renderPanel(GuiGraphics graphics) {
            drawRectangle(graphics, panelX, panelY, panelHeight, PANEL_WIDTH);
            graphics.drawString(screen.getMinecraft().font, Component.translatable("screen.immersive_autopilot.auto_routes_title"),
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
            immersive_aircraft.cobalt.network.NetworkHandler.sendToServer(
                    new com.immersiveautopilot.network.C2SSetAutoRoutes(screen.getMenu().getVehicle().getId(), names, labels));
        }

        private void clearRoutes() {
            for (EditBox field : fields) {
                field.setValue("");
            }
            AutoRouteClient.setRoutes(screen.getMenu().getVehicle().getId(), List.of());
            immersive_aircraft.cobalt.network.NetworkHandler.sendToServer(
                    new com.immersiveautopilot.network.C2SSetAutoRoutes(screen.getMenu().getVehicle().getId(), List.of(), List.of()));
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

        private void drawRectangle(GuiGraphics context, int x, int y, int h, int w) {
            context.blit(TEXTURE, x, y, 176, 0, 16, 16, 512, 256);
            context.blit(TEXTURE, x + w - 16, y, 176 + 32, 0, 16, 16, 512, 256);
            context.blit(TEXTURE, x + w - 16, y + h - 16, 176 + 32, 32, 16, 16, 512, 256);
            context.blit(TEXTURE, x, y + h - 16, 176, 32, 16, 16, 512, 256);

            context.blit(TEXTURE, x + 16, y, w - 32, 16, 176 + 16, 0, 16, 16, 512, 256);
            context.blit(TEXTURE, x + 16, y + h - 16, w - 32, 16, 176 + 16, 32, 16, 16, 512, 256);
            context.blit(TEXTURE, x, y + 16, 16, h - 32, 176, 16, 16, 16, 512, 256);
            context.blit(TEXTURE, x + w - 16, y + 16, 16, h - 32, 176 + 32, 16, 16, 16, 512, 256);

            context.blit(TEXTURE, x + 16, y + 16, w - 32, h - 32, 176 + 16, 16, 16, 16, 512, 256);
        }
    }
}
