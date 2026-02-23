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
    private final List<RouteLink> links = new ArrayList<>();

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

    public List<RouteLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public void addWaypoint(RouteWaypoint waypoint) {
        if (waypoint != null) {
            waypoints.add(waypoint);
        }
    }

    public void removeLastWaypoint() {
        if (!waypoints.isEmpty()) {
            removeWaypointAt(waypoints.size() - 1);
        }
    }

    public void removeWaypointAt(int index) {
        if (index < 0 || index >= waypoints.size()) {
            return;
        }
        waypoints.remove(index);
        links.removeIf(link -> link.from() == index || link.to() == index);
        for (int i = 0; i < links.size(); i++) {
            RouteLink link = links.get(i);
            int from = link.from();
            int to = link.to();
            if (from > index) {
                from--;
            }
            if (to > index) {
                to--;
            }
            links.set(i, new RouteLink(from, to));
        }
    }

    public void addLink(int from, int to) {
        if (from < 0 || to < 0 || from >= waypoints.size() || to >= waypoints.size()) {
            return;
        }
        RouteLink link = new RouteLink(from, to);
        if (!links.contains(link)) {
            links.add(link);
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
        ListTag linkList = new ListTag();
        for (RouteLink link : links) {
            CompoundTag linkTag = new CompoundTag();
            linkTag.putInt("From", link.from());
            linkTag.putInt("To", link.to());
            linkList.add(linkTag);
        }
        tag.put("Links", linkList);
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
        if (tag.contains("Links", Tag.TAG_LIST)) {
            ListTag linkList = tag.getList("Links", Tag.TAG_COMPOUND);
            for (int i = 0; i < linkList.size(); i++) {
                CompoundTag linkTag = linkList.getCompound(i);
                program.addLink(linkTag.getInt("From"), linkTag.getInt("To"));
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
        buf.writeInt(links.size());
        for (RouteLink link : links) {
            buf.writeInt(link.from());
            buf.writeInt(link.to());
        }
    }

    public static RouteProgram readFromBuf(RegistryFriendlyByteBuf buf) {
        String name = buf.readUtf();
        int count = buf.readInt();
        List<RouteWaypoint> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(RouteWaypoint.readFromBuf(buf));
        }
        RouteProgram program = new RouteProgram(name, list);
        int linkCount = buf.readInt();
        for (int i = 0; i < linkCount; i++) {
            program.addLink(buf.readInt(), buf.readInt());
        }
        return program;
    }
}
