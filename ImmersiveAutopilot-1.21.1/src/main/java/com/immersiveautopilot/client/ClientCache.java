package com.immersiveautopilot.client;

import com.immersiveautopilot.data.AircraftSnapshot;
import com.immersiveautopilot.data.TowerState;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientCache {
    private static final Map<BlockPos, List<AircraftSnapshot>> AIRCRAFT_LISTS = new HashMap<>();
    private static final Map<BlockPos, TowerState> TOWER_STATES = new HashMap<>();

    private ClientCache() {
    }

    public static void setAircraftList(BlockPos pos, List<AircraftSnapshot> list) {
        AIRCRAFT_LISTS.put(pos, list);
    }

    public static List<AircraftSnapshot> getAircraftList(BlockPos pos) {
        return AIRCRAFT_LISTS.getOrDefault(pos, Collections.emptyList());
    }

    public static void setTowerState(TowerState state) {
        TOWER_STATES.put(state.getPos(), state);
    }

    public static TowerState getTowerState(BlockPos pos) {
        return TOWER_STATES.get(pos);
    }
}
