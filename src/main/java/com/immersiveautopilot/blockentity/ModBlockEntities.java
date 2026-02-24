package com.immersiveautopilot.blockentity;

import com.immersiveautopilot.ImmersiveAutopilot;
import com.immersiveautopilot.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ImmersiveAutopilot.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TowerBlockEntity>> TOWER = BLOCK_ENTITIES.register("tower",
            () -> BlockEntityType.Builder.of(TowerBlockEntity::new, ModBlocks.TOWER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadarBlockEntity>> RADAR = BLOCK_ENTITIES.register("radar",
            () -> BlockEntityType.Builder.of(RadarBlockEntity::new, ModBlocks.RADAR.get()).build(null));

    private ModBlockEntities() {
    }
}
