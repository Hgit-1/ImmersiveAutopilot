package com.immersiveautopilot.mixin;

import com.immersiveautopilot.route.AutopilotRouteHolder;
import com.immersiveautopilot.route.RouteProgram;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VehicleEntity.class)
public class VehicleEntityRouteMixin implements AutopilotRouteHolder {
    @Unique
    private RouteProgram immersiveAutopilot$route;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void immersiveAutopilot$saveRoute(CompoundTag tag, CallbackInfo ci) {
        if (immersiveAutopilot$route != null) {
            tag.put("AutopilotRoute", immersiveAutopilot$route.toTag());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void immersiveAutopilot$loadRoute(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("AutopilotRoute", Tag.TAG_COMPOUND)) {
            immersiveAutopilot$route = RouteProgram.fromTag(tag.getCompound("AutopilotRoute"));
        } else {
            immersiveAutopilot$route = null;
        }
    }

    @Override
    @Nullable
    public RouteProgram getAutopilotRoute() {
        return immersiveAutopilot$route;
    }

    @Override
    public void setAutopilotRoute(@Nullable RouteProgram program) {
        immersiveAutopilot$route = program;
    }
}
