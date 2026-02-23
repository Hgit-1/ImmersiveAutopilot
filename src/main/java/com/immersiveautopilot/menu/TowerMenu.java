package com.immersiveautopilot.menu;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TowerMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Level level;
    private final TowerBlockEntity tower;

    public TowerMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, (TowerBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public TowerMenu(int containerId, Inventory inventory, TowerBlockEntity tower) {
        super(ModMenus.TOWER_MENU.get(), containerId);
        this.tower = tower;
        this.level = inventory.player.level();
        this.pos = tower.getBlockPos();
    }

    public BlockPos getPos() {
        return pos;
    }

    public TowerBlockEntity getTower() {
        return tower;
    }

    @Override
    public boolean stillValid(Player player) {
        return level.getBlockEntity(pos) == tower && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
