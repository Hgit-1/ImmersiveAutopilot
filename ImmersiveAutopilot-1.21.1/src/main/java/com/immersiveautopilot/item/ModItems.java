package com.immersiveautopilot.item;

import com.immersiveautopilot.ImmersiveAutopilot;
import com.immersiveautopilot.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ImmersiveAutopilot.MOD_ID);

    public static final RegistryObject<Item> TOWER = ITEMS.register("tower",
            () -> new BlockItem(ModBlocks.TOWER.get(), new Item.Properties()));

    private ModItems() {
    }
}
