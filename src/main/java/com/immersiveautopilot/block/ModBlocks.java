package com.immersiveautopilot.block;

import com.immersiveautopilot.ImmersiveAutopilot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ImmersiveAutopilot.MOD_ID);

    public static final RegistryObject<Block> TOWER = BLOCKS.register("tower",
            () -> new TowerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).requiresCorrectToolForDrops()));

    private ModBlocks() {
    }
}
