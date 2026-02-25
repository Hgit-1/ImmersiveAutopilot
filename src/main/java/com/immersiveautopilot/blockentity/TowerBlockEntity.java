package com.immersiveautopilot.blockentity;

import com.immersiveautopilot.menu.TowerMenu;
import com.immersiveautopilot.route.RouteProgram;
import com.immersiveautopilot.route.RouteWaypoint;
import com.immersiveautopilot.config.WorldConfig;
import com.immersiveautopilot.config.WorldConfigData;
import com.immersiveautopilot.network.RouteOfferManager;
import com.immersiveautopilot.network.S2CRouteOfferToPilot;
import com.immersiveautopilot.route.RouteEntry;
import com.immersiveautopilot.blockentity.RadarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
    public static final int DEFAULT_SCAN_RANGE = 64;
    public static final int MAX_SCAN_RANGE = 1024;

    private int scanRange = DEFAULT_SCAN_RANGE;
    private int lastSyncedScanRange = DEFAULT_SCAN_RANGE;
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
    private final Map<UUID, UUID> activePilots = new HashMap<>();
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
        this.scanRange = computeScanRange();
        setChanged();
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
        scanRange = computeScanRange();
        if (scanRange != lastSyncedScanRange) {
            lastSyncedScanRange = scanRange;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        if (worldConfig == null && level instanceof ServerLevel serverLevel) {
            worldConfig = WorldConfig.get(serverLevel);
            // Scan range is fixed to base + radar bonuses.
        }
        boolean powered = isPowered();
        if (!powered) {
            if (wasPowered) {
                insideAirspace.clear();
                activePilots.clear();
            }
            wasPowered = false;
            return;
        }
        if (!wasPowered) {
            wasPowered = true;
            handleAirspaceMessages();
        }
        handleAirspaceMessages();
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
                    sendRouteOfferIfNeeded(vehicle, pilot, entries, true);
                }
                insideAirspace.put(vehicle.getUUID(), true);
            } else if (vehicle.getControllingPassenger() instanceof ServerPlayer pilot) {
                sendRouteOfferIfNeeded(vehicle, pilot, entries, false);
            } else {
                activePilots.remove(vehicle.getUUID());
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
            UUID lastPilot = activePilots.remove(uuid);
            if (lastPilot != null && level instanceof ServerLevel serverLevel) {
                Player found = serverLevel.getPlayerByUUID(lastPilot);
                if (found instanceof ServerPlayer pilot) {
                    String msg = formatText(exitText);
                    if (!msg.isBlank()) {
                        pilot.displayClientMessage(Component.literal(msg), false);
                    }
                    immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(
                            new com.immersiveautopilot.network.S2CAirspaceState(vehicle != null ? vehicle.getId() : -1, false),
                            pilot);
                }
            }
            return true;
        });
    }

    private void sendRouteOfferIfNeeded(VehicleEntity vehicle, ServerPlayer pilot, List<RouteEntry> entries, boolean entering) {
        UUID vehicleId = vehicle.getUUID();
        UUID pilotId = pilot.getUUID();
        UUID lastPilot = activePilots.get(vehicleId);
        boolean pilotChanged = lastPilot == null || !lastPilot.equals(pilotId);
        if (pilotChanged) {
            activePilots.put(vehicleId, pilotId);
        }

        if (entering || pilotChanged) {
            immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(
                    new com.immersiveautopilot.network.S2CAirspaceState(vehicle.getId(), true), pilot);
            if (!entries.isEmpty()) {
                RouteOfferManager.createOffer(pilotId, null, vehicle.getId(), entries, level.getGameTime());
                immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(
                        new S2CRouteOfferToPilot(vehicle.getId(), towerName, entries), pilot);
            }
            if (entering) {
                String msg = formatText(autoRequestText);
                if (!msg.isBlank()) {
                    pilot.displayClientMessage(Component.literal(msg), false);
                }
            }
        }
    }

    private int computeScanRange() {
        int base = DEFAULT_SCAN_RANGE;
        int bonus = 0;
        if (level != null) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        mutable.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                        if (level.getBlockEntity(mutable) instanceof com.immersiveautopilot.blockentity.RadarBlockEntity radar) {
                            bonus += radar.getRangeBonus();
                        } else if (level.getBlockState(mutable).is(com.immersiveautopilot.block.ModBlocks.RADAR.get())) {
                            bonus += com.immersiveautopilot.blockentity.RadarBlockEntity.BASE_BONUS;
                        }
                    }
                }
            }
        }
        int max = MAX_SCAN_RANGE;
        WorldConfigData config = getWorldConfig();
        if (config != null) {
            max = config.maxScanRange;
        }
        return Math.max(1, Math.min(max, base + bonus));
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
        boolean allowSpeed = hasAutoSupportNearby();
        List<RouteEntry> entries = new java.util.ArrayList<>();
        for (Map.Entry<String, RouteProgram> entry : presets.entrySet()) {
            RouteProgram limited = limitProgramToAirspace(entry.getValue(), allowSpeed);
            if (limited != null) {
                entries.add(new RouteEntry(entry.getKey(), limited));
            }
        }
        boolean hasActive = presets.containsKey(activeRoute.getName());
        if (!hasActive && activeRoute != null) {
            RouteProgram limited = limitProgramToAirspace(activeRoute, allowSpeed);
            if (limited != null) {
                entries.add(new RouteEntry(activeRoute.getName(), limited));
            }
        }
        return entries;
    }

    private RouteProgram limitProgramToAirspace(RouteProgram program, boolean allowSpeed) {
        if (program == null || level == null) {
            return null;
        }
        List<RouteWaypoint> points = program.getWaypoints();
        if (points.isEmpty()) {
            return null;
        }
        List<RouteWaypoint> filtered = new java.util.ArrayList<>();
        int[] remap = new int[points.size()];
        java.util.Arrays.fill(remap, -1);
        Vec3 center = Vec3.atCenterOf(worldPosition);
        var dim = level.dimension().location();
        for (int i = 0; i < points.size(); i++) {
            RouteWaypoint wp = points.get(i);
            if (!dim.equals(wp.getDimension())) {
                continue;
            }
            double dist = center.distanceTo(Vec3.atCenterOf(wp.getPos()));
            if (dist <= scanRange) {
                remap[i] = filtered.size();
                float speed = allowSpeed ? wp.getSpeed() : 1.0f;
                filtered.add(new RouteWaypoint(wp.getPos(), wp.getDimension(), speed, wp.getHoldSeconds()));
            }
        }
        if (filtered.isEmpty()) {
            return null;
        }
        RouteProgram limited = new RouteProgram(program.getName(), filtered);
        for (com.immersiveautopilot.route.RouteLink link : program.getLinks()) {
            int from = link.from();
            int to = link.to();
            if (from >= 0 && from < remap.length && to >= 0 && to < remap.length) {
                int newFrom = remap[from];
                int newTo = remap[to];
                if (newFrom >= 0 && newTo >= 0) {
                    limited.addLink(newFrom, newTo);
                }
            }
        }
        return limited;
    }

    private boolean hasAutoSupportNearby() {
        if (level == null) {
            return false;
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    mutable.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    if (level.getBlockEntity(mutable) instanceof RadarBlockEntity radar) {
                        if (radar.hasAutoSupportModule()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
        lastSyncedScanRange = scanRange;
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

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        loadAdditional(tag, provider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, provider);
        }
    }
}
