package com.immersiveautopilot.autopilot;

import com.immersiveautopilot.item.ModItems;
import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.entity.inventory.VehicleInventoryDescription;
import net.minecraft.world.item.ItemStack;

public final class AutopilotSupport {
    private AutopilotSupport() {
    }

    public static boolean hasAutopilot(VehicleEntity vehicle) {
        if (vehicle instanceof InventoryVehicleEntity inventory) {
            for (ItemStack stack : inventory.getSlots(VehicleInventoryDescription.UPGRADE)) {
                if (!stack.isEmpty() && stack.is(ModItems.AUTOPILOT_UNIT.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAutopilotEnabled(VehicleEntity vehicle) {
        if (vehicle instanceof AutopilotStateAccess access) {
            return access.immersiveAutopilot$isAutopilotEnabled();
        }
        return false;
    }

    public static void setAutopilotEnabled(VehicleEntity vehicle, boolean enabled) {
        if (vehicle instanceof AutopilotStateAccess access) {
            access.immersiveAutopilot$setAutopilotEnabled(enabled);
        }
    }
}
