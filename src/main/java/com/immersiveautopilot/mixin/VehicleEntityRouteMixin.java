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
    private RouteProgram immersiveAutopilot$primary;
    @Unique
    private RouteProgram immersiveAutopilot$backup;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void immersiveAutopilot$saveRoute(CompoundTag tag, CallbackInfo ci) {
        if (immersiveAutopilot$primary != null) {
            tag.put("AutopilotPrimary", immersiveAutopilot$primary.toTag());
        }
        if (immersiveAutopilot$backup != null) {
            tag.put("AutopilotBackup", immersiveAutopilot$backup.toTag());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void immersiveAutopilot$loadRoute(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("AutopilotPrimary", Tag.TAG_COMPOUND)) {
            immersiveAutopilot$primary = RouteProgram.fromTag(tag.getCompound("AutopilotPrimary"));
        } else {
            immersiveAutopilot$primary = null;
        }
        if (tag.contains("AutopilotBackup", Tag.TAG_COMPOUND)) {
            immersiveAutopilot$backup = RouteProgram.fromTag(tag.getCompound("AutopilotBackup"));
        } else {
            immersiveAutopilot$backup = null;
        }
    }

    @Override
    @Nullable
    public RouteProgram getAutopilotPrimary() {
        return immersiveAutopilot$primary;
    }

    @Override
    public void setAutopilotPrimary(@Nullable RouteProgram program) {
        immersiveAutopilot$primary = program;
    }

    @Override
    @Nullable
    public RouteProgram getAutopilotBackup() {
        return immersiveAutopilot$backup;
    }

    @Override
    public void setAutopilotBackup(@Nullable RouteProgram program) {
        immersiveAutopilot$backup = program;
    }
}
