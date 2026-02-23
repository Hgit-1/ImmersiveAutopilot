package com.immersiveautopilot.block;

import com.immersiveautopilot.ImmersiveAutopilot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ImmersiveAutopilot.MOD_ID);

    public static final DeferredHolder<Block, Block> TOWER = BLOCKS.register("tower",
            () -> new TowerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).requiresCorrectToolForDrops()));

    private ModBlocks() {
    }
}
