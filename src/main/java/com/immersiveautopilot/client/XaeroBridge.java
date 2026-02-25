package com.immersiveautopilot.client;

import com.immersiveautopilot.route.RouteProgram;
import com.immersiveautopilot.route.RouteWaypoint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

public final class XaeroBridge {
    private static final String NAME_PREFIX = "IA:";

    private XaeroBridge() {
    }

    public static boolean isAvailable() {
        try {
            Class.forName("xaero.common.XaeroMinimapSession");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void syncTemporaryWaypoints(RouteProgram primary, RouteProgram backup) {
        syncTemporaryWaypoints(primary, backup, 0, 0, "", "");
    }

    public static void syncTemporaryWaypoints(RouteProgram primary, RouteProgram backup, int primaryCompleted, int backupCompleted) {
        syncTemporaryWaypoints(primary, backup, primaryCompleted, backupCompleted, "", "");
    }

    public static void syncTemporaryWaypoints(RouteProgram primary, RouteProgram backup, int primaryCompleted, int backupCompleted,
                                              String primaryLabel, String backupLabel) {
        try {
            Object set = getCurrentWaypointSet();
            if (set == null) {
                return;
            }
            clearOurTemporaryWaypoints(set);
            addProgramWaypoints(set, primary, "P", "AQUA", primaryCompleted, "GREEN", primaryLabel);
            addProgramWaypoints(set, backup, "B", "PURPLE", backupCompleted, "GREEN", backupLabel);
        } catch (Throwable ignored) {
        }
    }

    public static void clearTemporaryWaypoints() {
        try {
            Object set = getCurrentWaypointSet();
            if (set == null) {
                return;
            }
            clearOurTemporaryWaypoints(set);
        } catch (Throwable ignored) {
        }
    }

    public static boolean renderMinimap(Object guiGraphics, int x, int y, int width, int height, float partialTick) {
        return renderMinimap(guiGraphics, x, y, width, height, partialTick, false, false);
    }

    public static boolean renderMinimap(Object guiGraphics, int x, int y, int width, int height, float partialTick, boolean lockNorth, boolean disableRadar) {
        SettingOverride lockOverride = null;
        SettingOverride radarOverride = null;
        try {
            if (lockNorth || disableRadar) {
                Object settings = getModSettings();
                if (settings == null) {
                    return false;
                }
                if (lockNorth) {
                    lockOverride = applyBooleanSetting(settings, "lockNorth", true);
                    if (lockOverride == null) {
                        return false;
                    }
                }
                if (disableRadar) {
                    radarOverride = applyBooleanSetting(settings, "entityRadar", false);
                    if (radarOverride == null) {
                        return false;
                    }
                }
            }

            Class<?> sessionClass = Class.forName("xaero.common.XaeroMinimapSession");
            Method getCurrentSession = sessionClass.getMethod("getCurrentSession");
            Object session = getCurrentSession.invoke(null);
            if (session == null) {
                return false;
            }
            Method getMinimapProcessor = sessionClass.getMethod("getMinimapProcessor");
            Object processor = getMinimapProcessor.invoke(session);
            if (processor == null) {
                return false;
            }

            Class<?> consumersClass = Class.forName("xaero.common.graphics.CustomVertexConsumers");
            Object consumers = consumersClass.getConstructor().newInstance();

            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Method getInstance = mcClass.getMethod("getInstance");
            Object mc = getInstance.invoke(null);
            Method getWindow = mcClass.getMethod("getWindow");
            Object window = getWindow.invoke(mc);
            Method getScaledWidth = window.getClass().getMethod("getGuiScaledWidth");
            Method getScaledHeight = window.getClass().getMethod("getGuiScaledHeight");
            int screenW = (int) getScaledWidth.invoke(window);
            int screenH = (int) getScaledHeight.invoke(window);

            Method onRender = processor.getClass().getMethod(
                    "onRender",
                    Class.forName("net.minecraft.client.gui.GuiGraphics"),
                    int.class, int.class, int.class, int.class,
                    double.class, int.class, int.class, float.class, consumersClass
            );
            onRender.invoke(processor, guiGraphics, x, y, width, height, 1.0d, screenW, screenH, partialTick, consumers);
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (radarOverride != null) {
                radarOverride.restore();
            }
            if (lockOverride != null) {
                lockOverride.restore();
            }
        }
    }

    private static Object getModSettings() {
        try {
            Class<?> hudModClass = Class.forName("xaero.common.HudMod");
            Object hudMod = hudModClass.getField("INSTANCE").get(null);
            Method getSettings = hudModClass.getMethod("getSettings");
            return getSettings.invoke(hudMod);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static SettingOverride applyBooleanSetting(Object settings, String fieldName, boolean value) {
        try {
            var field = settings.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object fieldValue = field.get(settings);
            if (field.getType() == boolean.class) {
                boolean prev = field.getBoolean(settings);
                field.setBoolean(settings, value);
                return new SettingOverride(settings, field, prev);
            }
            if (fieldValue instanceof Boolean) {
                boolean prev = (Boolean) fieldValue;
                field.set(settings, value);
                return new SettingOverride(settings, field, prev);
            }
            if (fieldValue != null) {
                Method setter = findBooleanSetter(fieldValue.getClass());
                Method getter = findBooleanGetter(fieldValue.getClass());
                if (setter != null) {
                    Object prev = getter != null ? getter.invoke(fieldValue) : null;
                    setter.invoke(fieldValue, value);
                    return new SettingOverride(fieldValue, setter, getter, prev);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Method findBooleanSetter(Class<?> type) {
        for (String name : new String[]{"setValue", "set", "setBoolean"}) {
            try {
                return type.getMethod(name, boolean.class);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                return type.getMethod(name, Boolean.class);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Method findBooleanGetter(Class<?> type) {
        for (String name : new String[]{"getValue", "get", "is"} ) {
            try {
                return type.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static final class SettingOverride {
        private final Object target;
        private final java.lang.reflect.Field field;
        private final Method setter;
        private final Method getter;
        private final Object previous;

        private SettingOverride(Object target, java.lang.reflect.Field field, Object previous) {
            this.target = target;
            this.field = field;
            this.previous = previous;
            this.setter = null;
            this.getter = null;
        }

        private SettingOverride(Object target, Method setter, Method getter, Object previous) {
            this.target = target;
            this.setter = setter;
            this.getter = getter;
            this.previous = previous;
            this.field = null;
        }

        private void restore() {
            try {
                if (field != null) {
                    field.setAccessible(true);
                    if (field.getType() == boolean.class && previous instanceof Boolean) {
                        field.setBoolean(target, (Boolean) previous);
                    } else {
                        field.set(target, previous);
                    }
                } else if (setter != null && previous != null) {
                    setter.invoke(target, previous);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static Object getCurrentWaypointSet() throws Exception {
        Class<?> sessionClass = Class.forName("xaero.common.XaeroMinimapSession");
        Method getCurrentSession = sessionClass.getMethod("getCurrentSession");
        Object session = getCurrentSession.invoke(null);
        if (session == null) {
            return null;
        }
        Method getWaypointsManager = sessionClass.getMethod("getWaypointsManager");
        Object manager = getWaypointsManager.invoke(session);
        if (manager == null) {
            return null;
        }
        Method getCurrentWorld = manager.getClass().getMethod("getCurrentWorld");
        Object world = getCurrentWorld.invoke(manager);
        if (world == null) {
            return null;
        }
        Method getCurrentSet = world.getClass().getMethod("getCurrentSet");
        return getCurrentSet.invoke(world);
    }

    private static void clearOurTemporaryWaypoints(Object set) throws Exception {
        Method getList = set.getClass().getMethod("getList");
        Object listObj = getList.invoke(set);
        if (!(listObj instanceof List<?> list)) {
            return;
        }
        Iterator<?> iterator = list.iterator();
        while (iterator.hasNext()) {
            Object wp = iterator.next();
            if (wp == null) {
                continue;
            }
            Method getName = wp.getClass().getMethod("getName");
            Method isTemporary = wp.getClass().getMethod("isTemporary");
            Object nameObj = getName.invoke(wp);
            Object tempObj = isTemporary.invoke(wp);
            String name = nameObj instanceof String ? (String) nameObj : "";
            boolean temp = tempObj instanceof Boolean && (Boolean) tempObj;
            if (temp && name.startsWith(NAME_PREFIX)) {
                iterator.remove();
            }
        }
    }

    private static void addProgramWaypoints(Object set, RouteProgram program, String labelPrefix, String colorName, int completedCount,
                                            String completedColor, String labelOverride) throws Exception {
        if (program == null) {
            return;
        }
        Class<?> waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
        Class<?> colorClass = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
        Class<?> purposeClass = Class.forName("xaero.hud.minimap.waypoint.WaypointPurpose");

        @SuppressWarnings("unchecked")
        Object color = Enum.valueOf((Class<? extends Enum>) colorClass, colorName);
        @SuppressWarnings("unchecked")
        Object completeColor = Enum.valueOf((Class<? extends Enum>) colorClass, completedColor);
        @SuppressWarnings("unchecked")
        Object purpose = Enum.valueOf((Class<? extends Enum>) purposeClass, "NORMAL");

        Constructor<?> ctor = waypointClass.getConstructor(
                int.class, int.class, int.class, String.class, String.class,
                colorClass, purposeClass, boolean.class, boolean.class
        );
        Method add = set.getClass().getMethod("add", waypointClass);
        Method setTemporary = waypointClass.getMethod("setTemporary", boolean.class);
        Method setYIncluded = waypointClass.getMethod("setYIncluded", boolean.class);

        int index = 1;
        String baseName = labelOverride != null && !labelOverride.isBlank() ? labelOverride : program.getName();
        for (RouteWaypoint wp : program.getWaypoints()) {
            boolean completed = index <= completedCount;
            Object useColor = completed ? completeColor : color;
            String name = NAME_PREFIX + labelPrefix + "-" + baseName + "-" + index;
            String initials = (labelOverride != null && !labelOverride.isBlank() ? labelOverride : labelPrefix) + index;
            Object xaeroWp = ctor.newInstance(
                    wp.getPos().getX(),
                    wp.getPos().getY(),
                    wp.getPos().getZ(),
                    name,
                    initials,
                    useColor,
                    purpose,
                    true,
                    true
            );
            setTemporary.invoke(xaeroWp, true);
            setYIncluded.invoke(xaeroWp, true);
            add.invoke(set, xaeroWp);
            index++;
        }
    }
}
