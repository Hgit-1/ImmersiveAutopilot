package com.immersiveautopilot;

import com.immersiveautopilot.block.ModBlocks;
import com.immersiveautopilot.blockentity.ModBlockEntities;
import com.immersiveautopilot.item.ModItems;
import com.immersiveautopilot.menu.ModMenus;
import com.immersiveautopilot.network.ModNetwork;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.bus.api.IEventBus;

@Mod(ImmersiveAutopilot.MOD_ID)
public class ImmersiveAutopilot {
    public static final String MOD_ID = "immersive_autopilot";

    public ImmersiveAutopilot() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        ModNetwork.register();
    }
}
