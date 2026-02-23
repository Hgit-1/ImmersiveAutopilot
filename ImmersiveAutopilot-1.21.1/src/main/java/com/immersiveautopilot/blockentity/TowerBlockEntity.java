package com.immersiveautopilot.blockentity;

import com.immersiveautopilot.menu.TowerMenu;
import com.immersiveautopilot.route.RouteProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TowerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DEFAULT_SCAN_RANGE = 256;
    public static final int MAX_SCAN_RANGE = 1024;

    private int scanRange = DEFAULT_SCAN_RANGE;
    private UUID boundAircraft;
    private RouteProgram activeRoute = new RouteProgram("default");
    private final Map<String, RouteProgram> presets = new HashMap<>();

    public TowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWER.get(), pos, state);
    }

    public int getScanRange() {
        return scanRange;
    }

    public void setScanRange(int scanRange) {
        int clamped = Math.max(1, Math.min(MAX_SCAN_RANGE, scanRange));
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ScanRange", scanRange);
        if (boundAircraft != null) {
            tag.putUUID("BoundAircraft", boundAircraft);
        }
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
    public void loadAdditional(CompoundTag tag) {
        super.loadAdditional(tag);
        scanRange = tag.getInt("ScanRange");
        if (tag.hasUUID("BoundAircraft")) {
            boundAircraft = tag.getUUID("BoundAircraft");
        } else {
            boundAircraft = null;
        }
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
