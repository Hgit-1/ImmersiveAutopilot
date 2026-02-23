package com.immersiveautopilot.route;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteProgram {
    private final String name;
    private final List<RouteWaypoint> waypoints = new ArrayList<>();

    public RouteProgram(String name) {
        this.name = name == null ? "" : name;
    }

    public RouteProgram(String name, List<RouteWaypoint> waypoints) {
        this.name = name == null ? "" : name;
        if (waypoints != null) {
            this.waypoints.addAll(waypoints);
        }
    }

    public String getName() {
        return name;
    }

    public List<RouteWaypoint> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    public void addWaypoint(RouteWaypoint waypoint) {
        if (waypoint != null) {
            waypoints.add(waypoint);
        }
    }

    public void removeLastWaypoint() {
        if (!waypoints.isEmpty()) {
            waypoints.remove(waypoints.size() - 1);
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        ListTag list = new ListTag();
        for (RouteWaypoint waypoint : waypoints) {
            list.add(waypoint.toTag());
        }
        tag.put("Waypoints", list);
        return tag;
    }

    public static RouteProgram fromTag(CompoundTag tag) {
        String name = tag.getString("Name");
        RouteProgram program = new RouteProgram(name);
        if (tag.contains("Waypoints", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Waypoints", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                program.addWaypoint(RouteWaypoint.fromTag(list.getCompound(i)));
            }
        }
        return program;
    }

    public void writeToBuf(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeInt(waypoints.size());
        for (RouteWaypoint waypoint : waypoints) {
            waypoint.writeToBuf(buf);
        }
    }

    public static RouteProgram readFromBuf(RegistryFriendlyByteBuf buf) {
        String name = buf.readUtf();
        int count = buf.readInt();
        List<RouteWaypoint> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(RouteWaypoint.readFromBuf(buf));
        }
        return new RouteProgram(name, list);
    }
}
