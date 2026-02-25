package com.immersiveautopilot.data;

import com.immersiveautopilot.route.RouteProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final boolean autoSupport;
    private final List<String> presetNames;

    public TowerState(BlockPos pos, int scanRange, UUID boundAircraft, String boundName, RouteProgram activeRoute,
                      String towerName, String autoRequestText, String enterText, String exitText,
                      boolean targetAllInRange, boolean powered, boolean autoSupport, List<String> presetNames) {
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
        this.autoSupport = autoSupport;
        this.presetNames = presetNames == null ? List.of() : new ArrayList<>(presetNames);
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

    public boolean hasAutoSupport() {
        return autoSupport;
    }

    public List<String> getPresetNames() {
        return Collections.unmodifiableList(presetNames);
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
        buf.writeBoolean(autoSupport);
        buf.writeInt(presetNames.size());
        for (String name : presetNames) {
            buf.writeUtf(name == null ? "" : name);
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
        String towerName = buf.readUtf();
        String autoText = buf.readUtf();
        String enterText = buf.readUtf();
        String exitText = buf.readUtf();
        boolean targetAll = buf.readBoolean();
        boolean powered = buf.readBoolean();
        boolean autoSupport = buf.readBoolean();
        int presetCount = buf.readInt();
        List<String> presets = new ArrayList<>(Math.max(0, presetCount));
        for (int i = 0; i < presetCount; i++) {
            String preset = buf.readUtf();
            if (preset != null && !preset.isBlank()) {
                presets.add(preset);
            }
        }
        return new TowerState(pos, range, bound, boundName, route, towerName, autoText, enterText, exitText, targetAll, powered, autoSupport, presets);
    }
}
