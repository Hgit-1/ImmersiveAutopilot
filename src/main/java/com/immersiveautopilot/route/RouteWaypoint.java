package com.immersiveautopilot.route;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class RouteWaypoint {
    private final BlockPos pos;
    private final ResourceLocation dimension;
    private final float speed;
    private final int holdSeconds;

    public RouteWaypoint(BlockPos pos, ResourceLocation dimension, float speed, int holdSeconds) {
        this.pos = pos;
        this.dimension = dimension;
        this.speed = speed;
        this.holdSeconds = holdSeconds;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    public float getSpeed() {
        return speed;
    }

    public int getHoldSeconds() {
        return holdSeconds;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putString("Dim", dimension.toString());
        tag.putFloat("Speed", speed);
        tag.putInt("Hold", holdSeconds);
        return tag;
    }

    public static RouteWaypoint fromTag(CompoundTag tag) {
        BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        ResourceLocation dim = ResourceLocation.tryParse(tag.getString("Dim"));
        if (dim == null) {
            dim = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        }
        float speed = tag.getFloat("Speed");
        int hold = tag.getInt("Hold");
        return new RouteWaypoint(pos, dim, speed, hold);
    }

    public void writeToBuf(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeResourceLocation(dimension);
        buf.writeFloat(speed);
        buf.writeInt(holdSeconds);
    }

    public static RouteWaypoint readFromBuf(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ResourceLocation dim = buf.readResourceLocation();
        float speed = buf.readFloat();
        int hold = buf.readInt();
        return new RouteWaypoint(pos, dim, speed, hold);
    }
}
