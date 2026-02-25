package com.immersiveautopilot.client;

import com.immersiveautopilot.ImmersiveAutopilot;
import com.immersiveautopilot.blockentity.TowerBlockEntity;
import com.immersiveautopilot.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ImmersiveAutopilot.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class RadarRangeOverlay {
    private static final int GOLD = 0xFFFFD24A;
    private static final int GREEN = 0xFF41C96B;
    private static final int CYAN = 0xFF4FC3F7;

    private RadarRangeOverlay() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        if (!player.getMainHandItem().is(ModItems.RADAR_RANGE_SENSOR.get()) &&
                !player.getOffhandItem().is(ModItems.RADAR_RANGE_SENSOR.get())) {
            return;
        }
        Level level = player.level();
        List<TowerBlockEntity> towers = collectTowers(level, player.blockPosition().getX(), player.blockPosition().getZ(), 8);
        if (towers.isEmpty()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer fill = buffers.getBuffer(RenderType.translucent());

        int step = 4;
        int maxRange = 0;
        for (TowerBlockEntity tower : towers) {
            maxRange = Math.max(maxRange, tower.getScanRange());
        }
        int radius = Math.min(128, maxRange);
        int minX = player.blockPosition().getX() - radius;
        int maxX = player.blockPosition().getX() + radius;
        int minZ = player.blockPosition().getZ() - radius;
        int maxZ = player.blockPosition().getZ() + radius;

        for (int z = minZ; z <= maxZ; z += step) {
            for (int x = minX; x <= maxX; x += step) {
                int count = 0;
                for (TowerBlockEntity tower : towers) {
                    double dx = x + 0.5 - tower.getBlockPos().getX() - 0.5;
                    double dz = z + 0.5 - tower.getBlockPos().getZ() - 0.5;
                    if (dx * dx + dz * dz <= tower.getScanRange() * (double) tower.getScanRange()) {
                        count++;
                    }
                }
                if (count == 0) {
                    continue;
                }
                int color = count >= 2 ? GREEN : GOLD;
                int y = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        new net.minecraft.core.BlockPos(x, 0, z)).getY();
                drawQuad(fill, pose, camPos, x, y, z, step, color);
            }
        }

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (TowerBlockEntity tower : towers) {
            drawCircle(lines, pose, camPos, tower.getBlockPos().getX() + 0.5, tower.getBlockPos().getY() + 0.1,
                    tower.getBlockPos().getZ() + 0.5, tower.getScanRange(), CYAN);
        }

        buffers.endBatch();
    }

    private static void drawQuad(VertexConsumer consumer, PoseStack pose, Vec3 camPos, int x, int y, int z, int size, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 90;
        pose.pushPose();
        pose.translate(-camPos.x, -camPos.y, -camPos.z);
        var matrix = pose.last().pose();
        addQuadVertex(consumer, matrix, x, y + 0.02f, z, 0f, 0f, r, g, b, a);
        addQuadVertex(consumer, matrix, x + size, y + 0.02f, z, 1f, 0f, r, g, b, a);
        addQuadVertex(consumer, matrix, x + size, y + 0.02f, z + size, 1f, 1f, r, g, b, a);
        addQuadVertex(consumer, matrix, x, y + 0.02f, z + size, 0f, 1f, r, g, b, a);
        pose.popPose();
    }

    private static void addQuadVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                      float u, float v, int r, int g, int b, int a) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);
    }

    private static void drawCircle(VertexConsumer consumer, PoseStack pose, Vec3 camPos, double cx, double cy, double cz, double radius, int color) {
        int segments = 64;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        pose.pushPose();
        pose.translate(cx - camPos.x, cy - camPos.y, cz - camPos.z);
        var matrix = pose.last().pose();
        for (int i = 0; i < segments; i++) {
            double a0 = (2 * Math.PI * i) / segments;
            double a1 = (2 * Math.PI * (i + 1)) / segments;
            float x0 = (float) (Math.cos(a0) * radius);
            float z0 = (float) (Math.sin(a0) * radius);
            float x1 = (float) (Math.cos(a1) * radius);
            float z1 = (float) (Math.sin(a1) * radius);
            consumer.addVertex(matrix, x0, 0f, z0).setColor(r, g, b, 1f).setNormal(0f, 1f, 0f);
            consumer.addVertex(matrix, x1, 0f, z1).setColor(r, g, b, 1f).setNormal(0f, 1f, 0f);
        }
        pose.popPose();
    }

    private static List<TowerBlockEntity> collectTowers(Level level, int centerX, int centerZ, int chunkRadius) {
        List<TowerBlockEntity> list = new ArrayList<>();
        int centerChunkX = centerX >> 4;
        int centerChunkZ = centerZ >> 4;
        for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                LevelChunk chunk = level.getChunk(centerChunkX + dx, centerChunkZ + dz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof TowerBlockEntity tower) {
                        list.add(tower);
                    }
                }
            }
        }
        return list;
    }
}
