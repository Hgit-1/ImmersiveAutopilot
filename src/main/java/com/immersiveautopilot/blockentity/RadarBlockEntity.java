package com.immersiveautopilot.blockentity;

import com.immersiveautopilot.item.ModItems;
import com.immersiveautopilot.menu.RadarMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.ContainerHelper;
import org.jetbrains.annotations.Nullable;

public class RadarBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int BASE_BONUS = 64;
    private static final int SLOT_COUNT = 9;
    private static final int LENS_BONUS = 8;
    private static final int AMP_BONUS = 16;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR.get(), pos, state);
    }

    public int getRangeBonus() {
        int bonus = BASE_BONUS;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.RADAR_LENS.get())) {
                bonus += LENS_BONUS * stack.getCount();
            } else if (stack.is(ModItems.SIGNAL_AMPLIFIER.get())) {
                bonus += AMP_BONUS * stack.getCount();
            }
        }
        return bonus;
    }

    public boolean hasIdentModule() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.is(ModItems.RADAR_IDENT_MODULE.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.immersive_autopilot.radar");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RadarMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack stack = ContainerHelper.removeItem(items, index, count);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(items, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this &&
                player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }
}
