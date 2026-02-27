package com.immersiveautopilot.item;

import com.immersiveautopilot.ImmersiveAutopilot;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ImmersiveAutopilot.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.immersive_autopilot"))
                    .icon(() -> ModItems.TOWER.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.TOWER.get());
                        output.accept(ModItems.RADAR.get());
                        output.accept(ModItems.RADAR_LENS.get());
                        output.accept(ModItems.SIGNAL_AMPLIFIER.get());
                        output.accept(ModItems.RADAR_RANGE_SENSOR.get());
                        output.accept(ModItems.RADAR_IDENT_MODULE.get());
                        output.accept(ModItems.AUTOPILOT_UNIT.get());
                        output.accept(ModItems.TOWER_AUTO_SUPPORT.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
