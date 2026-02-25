package com.immersiveautopilot.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class AircraftSnapshot {
    private final int entityId;
    private final UUID uuid;
    private final String name;
    private final ResourceLocation typeId;
    private final double distance;
    private final double altitude;
    private final double speed;
    private final float enginePower;
    private final float fuelUtilization;
    private final float health;
    private final double posX;
    private final double posZ;
    private final double velX;
    private final double velY;
    private final double velZ;

    public AircraftSnapshot(int entityId, UUID uuid, String name, ResourceLocation typeId, double distance, double altitude, double speed,
                            float enginePower, float fuelUtilization, float health, double posX, double posZ, double velX, double velY, double velZ) {
        this.entityId = entityId;
        this.uuid = uuid;
        this.name = name;
        this.typeId = typeId;
        this.distance = distance;
        this.altitude = altitude;
        this.speed = speed;
        this.enginePower = enginePower;
        this.fuelUtilization = fuelUtilization;
        this.health = health;
        this.posX = posX;
        this.posZ = posZ;
        this.velX = velX;
        this.velY = velY;
        this.velZ = velZ;
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getTypeId() {
        return typeId;
    }

    public double getDistance() {
        return distance;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getSpeed() {
        return speed;
    }

    public float getEnginePower() {
        return enginePower;
    }

    public float getFuelUtilization() {
        return fuelUtilization;
    }

    public float getHealth() {
        return health;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosZ() {
        return posZ;
    }

    public double getVelX() {
        return velX;
    }

    public double getVelY() {
        return velY;
    }

    public double getVelZ() {
        return velZ;
    }

    public void writeToBuf(RegistryFriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUUID(uuid);
        buf.writeUtf(name);
        buf.writeResourceLocation(typeId);
        buf.writeDouble(distance);
        buf.writeDouble(altitude);
        buf.writeDouble(speed);
        buf.writeFloat(enginePower);
        buf.writeFloat(fuelUtilization);
        buf.writeFloat(health);
        buf.writeDouble(posX);
        buf.writeDouble(posZ);
        buf.writeDouble(velX);
        buf.writeDouble(velY);
        buf.writeDouble(velZ);
    }

    public static AircraftSnapshot readFromBuf(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readInt();
        UUID uuid = buf.readUUID();
        String name = buf.readUtf();
        ResourceLocation typeId = buf.readResourceLocation();
        double distance = buf.readDouble();
        double altitude = buf.readDouble();
        double speed = buf.readDouble();
        float enginePower = buf.readFloat();
        float fuel = buf.readFloat();
        float health = buf.readFloat();
        double posX = buf.readDouble();
        double posZ = buf.readDouble();
        double velX = buf.readDouble();
        double velY = buf.readDouble();
        double velZ = buf.readDouble();
        return new AircraftSnapshot(entityId, uuid, name, typeId, distance, altitude, speed, enginePower, fuel, health, posX, posZ, velX, velY, velZ);
    }
}
