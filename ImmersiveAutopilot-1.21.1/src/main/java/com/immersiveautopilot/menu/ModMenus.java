package com.immersiveautopilot.menu;

import com.immersiveautopilot.ImmersiveAutopilot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ImmersiveAutopilot.MOD_ID);

    public static final RegistryObject<MenuType<TowerMenu>> TOWER_MENU = MENUS.register("tower",
            () -> IMenuTypeExtension.create(TowerMenu::new));

    private ModMenus() {
    }
}
