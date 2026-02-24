package com.immersiveautopilot.item;

import com.immersiveautopilot.ImmersiveAutopilot;
import com.immersiveautopilot.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ImmersiveAutopilot.MOD_ID);

    public static final DeferredHolder<Item, Item> TOWER = ITEMS.register("tower",
            () -> new BlockItem(ModBlocks.TOWER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> RADAR = ITEMS.register("radar",
            () -> new BlockItem(ModBlocks.RADAR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> RADAR_LENS = ITEMS.register("radar_lens",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SIGNAL_AMPLIFIER = ITEMS.register("signal_amplifier",
            () -> new Item(new Item.Properties()));

    private ModItems() {
    }
}
