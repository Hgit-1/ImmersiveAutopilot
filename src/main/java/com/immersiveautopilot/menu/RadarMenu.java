package com.immersiveautopilot.menu;

import com.immersiveautopilot.blockentity.RadarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RadarMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Level level;
    private final RadarBlockEntity radar;

    public RadarMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, (RadarBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public RadarMenu(int containerId, Inventory inventory, RadarBlockEntity radar) {
        super(ModMenus.RADAR_MENU.get(), containerId);
        this.radar = radar;
        this.level = inventory.player.level();
        this.pos = radar.getBlockPos();

        int startX = 62;
        int startY = 20;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(radar, col + row * 3, startX + col * 18, startY + row * 18));
            }
        }

        int invY = 94;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }
        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, hotbarY));
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    public RadarBlockEntity getRadar() {
        return radar;
    }

    @Override
    public boolean stillValid(Player player) {
        return level.getBlockEntity(pos) == radar &&
                player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        int radarSlots = 9;
        if (index < radarSlots) {
            if (!moveItemStackTo(stack, radarSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, radarSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }
}
