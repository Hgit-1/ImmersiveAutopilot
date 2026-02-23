package com.immersiveautopilot.data;

import com.immersiveautopilot.route.RouteProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

public class TowerState {
    private final BlockPos pos;
    private final int scanRange;
    private final UUID boundAircraft;
    private final String boundName;
    private final RouteProgram activeRoute;

    public TowerState(BlockPos pos, int scanRange, UUID boundAircraft, String boundName, RouteProgram activeRoute) {
        this.pos = pos;
        this.scanRange = scanRange;
        this.boundAircraft = boundAircraft;
        this.boundName = boundName == null ? "" : boundName;
        this.activeRoute = activeRoute;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getScanRange() {
        return scanRange;
    }

    public UUID getBoundAircraft() {
        return boundAircraft;
    }

    public String getBoundName() {
        return boundName;
    }

    public RouteProgram getActiveRoute() {
        return activeRoute;
    }

    public void writeToBuf(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(scanRange);
        buf.writeBoolean(boundAircraft != null);
        if (boundAircraft != null) {
            buf.writeUUID(boundAircraft);
        }
        buf.writeUtf(boundName);
        if (activeRoute != null) {
            buf.writeBoolean(true);
            activeRoute.writeToBuf(buf);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static TowerState readFromBuf(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int range = buf.readInt();
        UUID bound = null;
        if (buf.readBoolean()) {
            bound = buf.readUUID();
        }
        String boundName = buf.readUtf();
        RouteProgram route = null;
        if (buf.readBoolean()) {
            route = RouteProgram.readFromBuf(buf);
        }
        return new TowerState(pos, range, bound, boundName, route);
    }
}
