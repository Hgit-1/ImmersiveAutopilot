package com.immersiveautopilot.blockentity;

import com.immersiveautopilot.ImmersiveAutopilot;
import com.immersiveautopilot.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ImmersiveAutopilot.MOD_ID);

    public static final RegistryObject<BlockEntityType<TowerBlockEntity>> TOWER = BLOCK_ENTITIES.register("tower",
            () -> BlockEntityType.Builder.of(TowerBlockEntity::new, ModBlocks.TOWER.get()).build(null));

    private ModBlockEntities() {
    }
}
