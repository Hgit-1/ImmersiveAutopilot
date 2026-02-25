package com.immersiveautopilot.item;

import com.immersiveautopilot.ImmersiveAutopilot;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {
    private ModCreativeTabs() {
    }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.TOWER.get());
            event.accept(ModItems.RADAR.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.RADAR_LENS.get());
            event.accept(ModItems.SIGNAL_AMPLIFIER.get());
            event.accept(ModItems.RADAR_RANGE_SENSOR.get());
            event.accept(ModItems.RADAR_IDENT_MODULE.get());
        }
    }
}
