package com.immersiveautopilot.blockentity;

import com.immersiveautopilot.menu.TowerMenu;
import com.immersiveautopilot.route.RouteProgram;
import com.immersiveautopilot.config.WorldConfig;
import com.immersiveautopilot.config.WorldConfigData;
import com.immersiveautopilot.network.RouteOfferManager;
import com.immersiveautopilot.network.S2CRouteOfferToPilot;
import com.immersiveautopilot.route.RouteEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import immersive_aircraft.entity.VehicleEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TowerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DEFAULT_SCAN_RANGE = 256;
    public static final int MAX_SCAN_RANGE = 1024;

    private int scanRange = DEFAULT_SCAN_RANGE;
    private WorldConfigData worldConfig;
    private UUID boundAircraft;
    private String towerName = "default_tower";
    private String autoRequestText = "Auto request from {tower}";
    private String enterText = "Entering {tower}";
    private String exitText = "Leaving {tower}";
    private boolean targetAllInRange = false;
    private RouteProgram activeRoute = new RouteProgram("default");
    private final Map<String, RouteProgram> presets = new HashMap<>();
    private final Map<UUID, Boolean> insideAirspace = new HashMap<>();
    private boolean wasPowered = false;

    public TowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWER.get(), pos, state);
    }

    private WorldConfigData getWorldConfig() {
        if (worldConfig != null) {
            return worldConfig;
        }
        if (level instanceof ServerLevel serverLevel) {
            worldConfig = WorldConfig.get(serverLevel);
        }
        return worldConfig;
    }

    public int getScanRange() {
        return scanRange;
    }

    public void setScanRange(int scanRange) {
        int max = MAX_SCAN_RANGE;
        int min = 1;
        WorldConfigData config = getWorldConfig();
        if (config != null) {
            max = config.maxScanRange;
        }
        int clamped = Math.max(min, Math.min(max, scanRange));
        if (this.scanRange != clamped) {
            this.scanRange = clamped;
            setChanged();
        }
    }

    @Nullable
    public UUID getBoundAircraft() {
        return boundAircraft;
    }

    public void setBoundAircraft(@Nullable UUID boundAircraft) {
        this.boundAircraft = boundAircraft;
        setChanged();
    }

    public String getTowerName() {
        return towerName;
    }

    public void setTowerName(String towerName) {
        if (towerName == null || towerName.isBlank()) {
            this.towerName = "default_tower";
        } else {
            this.towerName = towerName;
        }
        setChanged();
    }

    public String getAutoRequestText() {
        return autoRequestText;
    }

    public void setAutoRequestText(String autoRequestText) {
        this.autoRequestText = autoRequestText == null ? "" : autoRequestText;
        setChanged();
    }

    public String getEnterText() {
        return enterText;
    }

    public void setEnterText(String enterText) {
        this.enterText = enterText == null ? "" : enterText;
        setChanged();
    }

    public String getExitText() {
        return exitText;
    }

    public void setExitText(String exitText) {
        this.exitText = exitText == null ? "" : exitText;
        setChanged();
    }

    public boolean isTargetAllInRange() {
        return targetAllInRange;
    }

    public void setTargetAllInRange(boolean targetAllInRange) {
        this.targetAllInRange = targetAllInRange;
        setChanged();
    }

    public RouteProgram getActiveRoute() {
        return activeRoute;
    }

    public void setActiveRoute(RouteProgram activeRoute) {
        this.activeRoute = activeRoute == null ? new RouteProgram("default") : activeRoute;
        setChanged();
    }

    public Map<String, RouteProgram> getPresets() {
        return presets;
    }

    public void savePreset(String name, RouteProgram program) {
        if (name == null || name.isBlank() || program == null) {
            return;
        }
        presets.put(name, program);
        setChanged();
    }

    public RouteProgram loadPreset(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return presets.get(name);
    }

    public boolean isPowered() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    public void tickServer() {
        if (level == null) {
            return;
        }
        if (worldConfig == null && level instanceof ServerLevel serverLevel) {
            worldConfig = WorldConfig.get(serverLevel);
            if (scanRange == DEFAULT_SCAN_RANGE) {
                scanRange = worldConfig.defaultScanRange;
            }
        }
        boolean powered = isPowered();
        if (!powered) {
            if (wasPowered) {
                insideAirspace.clear();
            }
            wasPowered = false;
            return;
        }
        if (!wasPowered) {
            wasPowered = true;
            for (UUID uuid : sendAutoRequests()) {
                insideAirspace.put(uuid, true);
            }
        }
        handleAirspaceMessages();
    }

    private java.util.Set<UUID> sendAutoRequests() {
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        if (level == null) {
            return seen;
        }
        List<RouteEntry> entries = getRouteEntries();
        for (VehicleEntity vehicle : getTargetsInRange()) {
            seen.add(vehicle.getUUID());
            if (vehicle.getControllingPassenger() instanceof ServerPlayer pilot) {
                if (!entries.isEmpty()) {
                    RouteOfferManager.createOffer(pilot.getUUID(), null, vehicle.getId(), entries, level.getGameTime());
                    immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(
                            new S2CRouteOfferToPilot(vehicle.getId(), towerName, entries), pilot);
                }
                String msg = formatText(autoRequestText);
                if (!msg.isBlank()) {
                    pilot.displayClientMessage(Component.literal(msg), false);
                }
            }
        }
        return seen;
    }

    private void handleAirspaceMessages() {
        if (level == null) {
            return;
        }
        List<RouteEntry> entries = getRouteEntries();
        HashSet<UUID> current = new HashSet<>();
        for (VehicleEntity vehicle : getTargetsInRange()) {
            current.add(vehicle.getUUID());
            if (!insideAirspace.containsKey(vehicle.getUUID())) {
                if (vehicle.getControllingPassenger() instanceof ServerPlayer pilot) {
                    String msg = formatText(enterText);
                    if (!msg.isBlank()) {
                        pilot.displayClientMessage(Component.literal(msg), false);
                    }
                    if (!entries.isEmpty()) {
                        RouteOfferManager.createOffer(pilot.getUUID(), null, vehicle.getId(), entries, level.getGameTime());
                        immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(
                                new S2CRouteOfferToPilot(vehicle.getId(), towerName, entries), pilot);
                    }
                }
                insideAirspace.put(vehicle.getUUID(), true);
            }
        }
        insideAirspace.keySet().removeIf(uuid -> {
            if (current.contains(uuid)) {
                return false;
            }
            VehicleEntity vehicle = null;
            if (level instanceof ServerLevel serverLevel) {
                if (serverLevel.getEntity(uuid) instanceof VehicleEntity v) {
                    vehicle = v;
                }
            }
            if (vehicle != null && vehicle.getControllingPassenger() instanceof ServerPlayer pilot) {
                String msg = formatText(exitText);
                if (!msg.isBlank()) {
                    pilot.displayClientMessage(Component.literal(msg), false);
                }
            }
            return true;
        });
    }

    private Iterable<VehicleEntity> getTargetsInRange() {
        if (level == null) {
            return new HashSet<>();
        }
        AABB box = new AABB(worldPosition).inflate(scanRange);
        if (targetAllInRange) {
            return level.getEntitiesOfClass(VehicleEntity.class, box);
        }
        if (boundAircraft != null && level instanceof ServerLevel serverLevel) {
            if (serverLevel.getEntity(boundAircraft) instanceof VehicleEntity vehicle) {
                if (vehicle.position().distanceTo(Vec3.atCenterOf(worldPosition)) <= scanRange) {
                    return java.util.List.of(vehicle);
                }
            }
        }
        return new HashSet<>();
    }

    private String formatText(String raw) {
        String base = raw == null ? "" : raw;
        return base.replace("{tower}", towerName);
    }

    private List<RouteEntry> getRouteEntries() {
        List<RouteEntry> entries = new java.util.ArrayList<>();
        for (Map.Entry<String, RouteProgram> entry : presets.entrySet()) {
            entries.add(new RouteEntry(entry.getKey(), entry.getValue()));
        }
        boolean hasActive = presets.containsKey(activeRoute.getName());
        if (!hasActive && activeRoute != null) {
            entries.add(new RouteEntry(activeRoute.getName(), activeRoute));
        }
        return entries;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.immersive_autopilot.tower");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TowerMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("ScanRange", scanRange);
        if (boundAircraft != null) {
            tag.putUUID("BoundAircraft", boundAircraft);
        }
        tag.putString("TowerName", towerName);
        tag.putString("AutoRequestText", autoRequestText);
        tag.putString("EnterText", enterText);
        tag.putString("ExitText", exitText);
        tag.putBoolean("TargetAllInRange", targetAllInRange);
        tag.put("ActiveRoute", activeRoute.toTag());

        ListTag presetList = new ListTag();
        for (Map.Entry<String, RouteProgram> entry : presets.entrySet()) {
            CompoundTag presetTag = new CompoundTag();
            presetTag.putString("Name", entry.getKey());
            presetTag.put("Route", entry.getValue().toTag());
            presetList.add(presetTag);
        }
        tag.put("Presets", presetList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        scanRange = tag.getInt("ScanRange");
        if (tag.hasUUID("BoundAircraft")) {
            boundAircraft = tag.getUUID("BoundAircraft");
        } else {
            boundAircraft = null;
        }
        towerName = tag.contains("TowerName") ? tag.getString("TowerName") : "default_tower";
        autoRequestText = tag.contains("AutoRequestText") ? tag.getString("AutoRequestText") : "Auto request from {tower}";
        enterText = tag.contains("EnterText") ? tag.getString("EnterText") : "Entering {tower}";
        exitText = tag.contains("ExitText") ? tag.getString("ExitText") : "Leaving {tower}";
        targetAllInRange = tag.getBoolean("TargetAllInRange");
        if (tag.contains("ActiveRoute", Tag.TAG_COMPOUND)) {
            activeRoute = RouteProgram.fromTag(tag.getCompound("ActiveRoute"));
        } else {
            activeRoute = new RouteProgram("default");
        }
        presets.clear();
        if (tag.contains("Presets", Tag.TAG_LIST)) {
            ListTag presetList = tag.getList("Presets", Tag.TAG_COMPOUND);
            for (int i = 0; i < presetList.size(); i++) {
                CompoundTag presetTag = presetList.getCompound(i);
                String name = presetTag.getString("Name");
                if (presetTag.contains("Route", Tag.TAG_COMPOUND)) {
                    presets.put(name, RouteProgram.fromTag(presetTag.getCompound("Route")));
                }
            }
        }
    }
}
