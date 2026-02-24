package com.immersiveautopilot.client;

import com.immersiveautopilot.route.RouteProgram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientRouteCache {
    private static final Map<Integer, RouteProgram> PRIMARY = new ConcurrentHashMap<>();
    private static final Map<Integer, RouteProgram> BACKUP = new ConcurrentHashMap<>();

    private ClientRouteCache() {
    }

    public static void setRoutes(int vehicleId, RouteProgram primary, RouteProgram backup) {
        if (primary != null) {
            PRIMARY.put(vehicleId, primary);
        } else {
            PRIMARY.remove(vehicleId);
        }
        if (backup != null) {
            BACKUP.put(vehicleId, backup);
        } else {
            BACKUP.remove(vehicleId);
        }
    }

    public static RouteProgram getPrimary(int vehicleId) {
        return PRIMARY.get(vehicleId);
    }

    public static RouteProgram getBackup(int vehicleId) {
        return BACKUP.get(vehicleId);
    }
}
