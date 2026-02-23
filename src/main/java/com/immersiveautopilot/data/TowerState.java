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
    private final String towerName;
    private final String autoRequestText;
    private final String enterText;
    private final String exitText;
    private final boolean targetAllInRange;
    private final boolean powered;

    public TowerState(BlockPos pos, int scanRange, UUID boundAircraft, String boundName, RouteProgram activeRoute,
                      String towerName, String autoRequestText, String enterText, String exitText,
                      boolean targetAllInRange, boolean powered) {
        this.pos = pos;
        this.scanRange = scanRange;
        this.boundAircraft = boundAircraft;
        this.boundName = boundName == null ? "" : boundName;
        this.activeRoute = activeRoute;
        this.towerName = towerName == null ? "default_tower" : towerName;
        this.autoRequestText = autoRequestText == null ? "" : autoRequestText;
        this.enterText = enterText == null ? "" : enterText;
        this.exitText = exitText == null ? "" : exitText;
        this.targetAllInRange = targetAllInRange;
        this.powered = powered;
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

    public String getTowerName() {
        return towerName;
    }

    public String getAutoRequestText() {
        return autoRequestText;
    }

    public String getEnterText() {
        return enterText;
    }

    public String getExitText() {
        return exitText;
    }

    public boolean isTargetAllInRange() {
        return targetAllInRange;
    }

    public boolean isPowered() {
        return powered;
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
        buf.writeUtf(towerName);
        buf.writeUtf(autoRequestText);
        buf.writeUtf(enterText);
        buf.writeUtf(exitText);
        buf.writeBoolean(targetAllInRange);
        buf.writeBoolean(powered);
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
        String towerName = buf.readUtf();
        String autoText = buf.readUtf();
        String enterText = buf.readUtf();
        String exitText = buf.readUtf();
        boolean targetAll = buf.readBoolean();
        boolean powered = buf.readBoolean();
        return new TowerState(pos, range, bound, boundName, route, towerName, autoText, enterText, exitText, targetAll, powered);
    }
}
