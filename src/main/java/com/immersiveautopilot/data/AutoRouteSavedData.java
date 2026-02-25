package com.immersiveautopilot.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AutoRouteSavedData extends SavedData {
    public record Entry(String name, String label) {
    }

    private static final String DATA_NAME = "immersive_autopilot_auto_routes";
    private final Map<UUID, List<Entry>> routes = new HashMap<>();

    public static AutoRouteSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(AutoRouteSavedData::new, AutoRouteSavedData::load),
            DATA_NAME
        );
    }

    public List<Entry> getRoutes(UUID vehicleId) {
        return routes.getOrDefault(vehicleId, List.of());
    }

    public void setRoutes(UUID vehicleId, List<Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            routes.remove(vehicleId);
        } else {
            routes.put(vehicleId, new ArrayList<>(entries));
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, List<Entry>> entry : routes.entrySet()) {
            CompoundTag vehicleTag = new CompoundTag();
            vehicleTag.putUUID("Id", entry.getKey());
            ListTag routesTag = new ListTag();
            for (Entry route : entry.getValue()) {
                CompoundTag routeTag = new CompoundTag();
                routeTag.putString("Name", route.name());
                if (route.label() != null && !route.label().isBlank()) {
                    routeTag.putString("Label", route.label());
                }
                routesTag.add(routeTag);
            }
            vehicleTag.put("Routes", routesTag);
            list.add(vehicleTag);
        }
        tag.put("Vehicles", list);
        return tag;
    }

    public static AutoRouteSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        AutoRouteSavedData data = new AutoRouteSavedData();
        if (tag.contains("Vehicles", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Vehicles", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag vehicleTag = list.getCompound(i);
                if (!vehicleTag.hasUUID("Id")) {
                    continue;
                }
                UUID id = vehicleTag.getUUID("Id");
                List<Entry> entries = new ArrayList<>();
                if (vehicleTag.contains("Routes", Tag.TAG_LIST)) {
                    ListTag routesTag = vehicleTag.getList("Routes", Tag.TAG_COMPOUND);
                    for (int j = 0; j < routesTag.size(); j++) {
                        CompoundTag routeTag = routesTag.getCompound(j);
                        String name = routeTag.getString("Name");
                        String label = routeTag.getString("Label");
                        if (name != null && !name.isBlank()) {
                            entries.add(new Entry(name, label == null ? "" : label));
                        }
                    }
                }
                if (!entries.isEmpty()) {
                    data.routes.put(id, entries);
                }
            }
        }
        return data;
    }
}
