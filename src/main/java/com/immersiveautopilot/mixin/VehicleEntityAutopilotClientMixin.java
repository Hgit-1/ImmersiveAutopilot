package com.immersiveautopilot.mixin;

import com.immersiveautopilot.autopilot.AutopilotSupport;
import com.immersiveautopilot.client.ClientRouteGuidance;
import com.immersiveautopilot.route.RouteWaypoint;
import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.network.c2s.EnginePowerMessage;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VehicleEntity.class)
public class VehicleEntityAutopilotClientMixin {
    @Unique
    private float immersiveAutopilot$lastSentTarget = -1.0f;
    @Unique
    private int immersiveAutopilot$lastSentTick = 0;

    @Inject(method = "tickPilot", at = @At("TAIL"))
    private void immersiveAutopilot$applyAutopilot(CallbackInfo ci) {
        VehicleEntity vehicle = (VehicleEntity) (Object) this;
        if (!vehicle.level().isClientSide) {
            return;
        }
        Entity pilot = vehicle.getControllingPassenger();
        if (!(pilot instanceof Player player) || !player.isLocalPlayer()) {
            return;
        }
        if (!AutopilotSupport.isAutopilotEnabled(vehicle)) {
            return;
        }
        if (!AutopilotSupport.hasAutopilot(vehicle)) {
            return;
        }
        if (!ClientRouteGuidance.isActiveFor(vehicle.getId())) {
            return;
        }
        RouteWaypoint target = ClientRouteGuidance.getCurrentTarget();
        if (target == null || !target.getDimension().equals(player.level().dimension().location())) {
            return;
        }
        Vec3 pos = vehicle.position();
        Vec3 targetPos = Vec3.atCenterOf(target.getPos());
        Vec3 toTarget = targetPos.subtract(pos);
        if (toTarget.lengthSqr() < 1.0E-4) {
            return;
        }

        Vector3f forward = vehicle.getForwardDirection();
        Vec3 forwardVec = new Vec3(forward.x(), forward.y(), forward.z());
        Vec3 forwardFlat = new Vec3(forwardVec.x, 0.0, forwardVec.z);
        Vec3 targetFlat = new Vec3(toTarget.x, 0.0, toTarget.z);
        if (forwardFlat.lengthSqr() < 1.0E-6 || targetFlat.lengthSqr() < 1.0E-6) {
            return;
        }

        forwardFlat = forwardFlat.normalize();
        targetFlat = targetFlat.normalize();
        double cross = forwardFlat.x * targetFlat.z - forwardFlat.z * targetFlat.x;
        double dot = forwardFlat.x * targetFlat.x + forwardFlat.z * targetFlat.z;
        double yawError = Math.atan2(cross, dot);
        float movementX = (float) Mth.clamp(-yawError / (Math.PI / 4.0), -1.0, 1.0);

        Vec3 targetNorm = toTarget.normalize();
        double desiredPitch = Math.asin(Mth.clamp(targetNorm.y, -1.0, 1.0));
        Vec3 forwardNorm = forwardVec.normalize();
        double currentPitch = Math.asin(Mth.clamp(forwardNorm.y, -1.0, 1.0));
        double pitchError = desiredPitch - currentPitch;
        float movementZ = (float) Mth.clamp(-pitchError / (Math.PI / 6.0), -1.0, 1.0);

        float movementY = 0.0f;
        float targetSpeed = 1.0f;
        if (vehicle instanceof EngineVehicle engineVehicle) {
            targetSpeed = Mth.clamp(target.getSpeed(), 0.0f, 1.0f);
        }

        if (vehicle.onGround() && toTarget.y > 2.0) {
            double speed = vehicle.getDeltaMovement().length();
            movementY = Math.max(movementY, 1.0f);
            if (speed > 0.25) {
                movementZ = Math.min(movementZ, -0.8f);
            } else {
                movementZ = Math.max(movementZ, -0.2f);
            }
        }

        vehicle.setInputs(movementX, movementY, movementZ);

        if (vehicle instanceof EngineVehicle engineVehicle) {
            float current = engineVehicle.getEngineTarget();
            float blended = Mth.lerp(0.08f, current, targetSpeed);
            if (Math.abs(current - blended) > 0.001f) {
                engineVehicle.setEngineTarget(blended);
            }
            immersiveAutopilot$lastSentTick++;
            if (Math.abs(immersiveAutopilot$lastSentTarget - blended) > 0.02f || immersiveAutopilot$lastSentTick >= 10) {
                NetworkHandler.sendToServer(new EnginePowerMessage(blended));
                immersiveAutopilot$lastSentTarget = blended;
                immersiveAutopilot$lastSentTick = 0;
            }
        }
    }
}
