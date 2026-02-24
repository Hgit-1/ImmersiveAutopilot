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
        try {
            Object set = getCurrentWaypointSet();
            if (set == null) {
                return;
            }
            clearOurTemporaryWaypoints(set);
            addProgramWaypoints(set, primary, "P", "AQUA");
            addProgramWaypoints(set, backup, "B", "GOLD");
        } catch (Throwable ignored) {
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

    private static void addProgramWaypoints(Object set, RouteProgram program, String labelPrefix, String colorName) throws Exception {
        if (program == null) {
            return;
        }
        Class<?> waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
        Class<?> colorClass = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
        Class<?> purposeClass = Class.forName("xaero.hud.minimap.waypoint.WaypointPurpose");

        @SuppressWarnings("unchecked")
        Object color = Enum.valueOf((Class<? extends Enum>) colorClass, colorName);
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
        for (RouteWaypoint wp : program.getWaypoints()) {
            String name = NAME_PREFIX + labelPrefix + "-" + program.getName() + "-" + index;
            String initials = labelPrefix + index;
            Object xaeroWp = ctor.newInstance(
                    wp.getPos().getX(),
                    wp.getPos().getY(),
                    wp.getPos().getZ(),
                    name,
                    initials,
                    color,
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
