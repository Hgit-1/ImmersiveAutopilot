package com.immersiveautopilot.client;

import com.immersiveautopilot.network.C2SPilotRouteDecision;
import com.immersiveautopilot.route.RouteEntry;
import immersive_aircraft.cobalt.network.NetworkHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoRouteClient {
    public record Entry(String routeName, String label) {
    }

    private static final Map<Integer, List<Entry>> ROUTES = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> INDICES = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> ACCEPTED = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    private AutoRouteClient() {
    }

    public static void setRoutes(int vehicleId, List<Entry> entries) {
        ROUTES.put(vehicleId, entries == null ? List.of() : new ArrayList<>(entries));
        INDICES.put(vehicleId, 0);
        ACCEPTED.put(vehicleId, false);
    }

    public static void requestRoutes(int vehicleId) {
        long now = System.currentTimeMillis();
        long last = LAST_REQUEST.getOrDefault(vehicleId, 0L);
        if (now - last < 2000L) {
            return;
        }
        LAST_REQUEST.put(vehicleId, now);
        immersive_aircraft.cobalt.network.NetworkHandler.sendToServer(
                new com.immersiveautopilot.network.C2SRequestAutoRoutes(vehicleId));
    }

    public static List<Entry> getRoutes(int vehicleId) {
        return ROUTES.getOrDefault(vehicleId, Collections.emptyList());
    }

    public static int getIndex(int vehicleId) {
        return INDICES.getOrDefault(vehicleId, 0);
    }

    public static boolean tryAutoAccept(int vehicleId, List<RouteEntry> offers) {
        List<Entry> entries = ROUTES.get(vehicleId);
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        int index = INDICES.getOrDefault(vehicleId, 0);
        if (index < 0 || index >= entries.size()) {
            return false;
        }
        String desired = entries.get(index).routeName();
        if (desired == null || desired.isBlank()) {
            return false;
        }
        for (RouteEntry entry : offers) {
            if (desired.equals(entry.name())) {
                ClientRouteCache.setRoutes(vehicleId, entry.program(), null);
                String label = entries.get(index).label();
                ClientRouteGuidance.acceptRoutes(vehicleId, entry.program(), null, label == null ? "" : label, "");
                NetworkHandler.sendToServer(new C2SPilotRouteDecision(vehicleId, true, entry.name(), ""));
                ACCEPTED.put(vehicleId, true);
                return true;
            }
        }
        return false;
    }

    public static void onAirspaceEnter(int vehicleId) {
        ACCEPTED.put(vehicleId, false);
        requestRoutes(vehicleId);
    }

    public static void onAirspaceExit(int vehicleId) {
        if (!Boolean.TRUE.equals(ACCEPTED.get(vehicleId))) {
            return;
        }
        int index = INDICES.getOrDefault(vehicleId, 0);
        List<Entry> entries = ROUTES.get(vehicleId);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        if (index < entries.size() - 1) {
            INDICES.put(vehicleId, index + 1);
        }
        ACCEPTED.put(vehicleId, false);
    }
}
