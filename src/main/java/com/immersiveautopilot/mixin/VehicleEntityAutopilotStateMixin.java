package com.immersiveautopilot.mixin;

import com.immersiveautopilot.autopilot.AutopilotStateAccess;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VehicleEntity.class)
public class VehicleEntityAutopilotStateMixin implements AutopilotStateAccess {
    @Unique
    private static final EntityDataAccessor<Boolean> IMMERSIVE_AUTOPILOT_ENABLED =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void immersiveAutopilot$defineData(CallbackInfo ci) {
        ((VehicleEntity) (Object) this).getEntityData().define(IMMERSIVE_AUTOPILOT_ENABLED, false);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void immersiveAutopilot$save(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("AutopilotEnabled", immersiveAutopilot$isAutopilotEnabled());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void immersiveAutopilot$load(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("AutopilotEnabled")) {
            immersiveAutopilot$setAutopilotEnabled(tag.getBoolean("AutopilotEnabled"));
        }
    }

    @Override
    public boolean immersiveAutopilot$isAutopilotEnabled() {
        return ((VehicleEntity) (Object) this).getEntityData().get(IMMERSIVE_AUTOPILOT_ENABLED);
    }

    @Override
    public void immersiveAutopilot$setAutopilotEnabled(boolean enabled) {
        ((VehicleEntity) (Object) this).getEntityData().set(IMMERSIVE_AUTOPILOT_ENABLED, enabled);
    }
}
