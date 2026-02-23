package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import com.immersiveautopilot.route.RouteProgram;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class C2SSavePreset extends Message {
    public static final CustomPacketPayload.Type<C2SSavePreset> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "save_preset"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSavePreset> STREAM_CODEC = StreamCodec.ofMember(C2SSavePreset::encode, C2SSavePreset::new);

    private final BlockPos pos;
    private final String name;
    private final RouteProgram program;

    public C2SSavePreset(BlockPos pos, String name, RouteProgram program) {
        this.pos = pos;
        this.name = name;
        this.program = program;
    }

    public C2SSavePreset(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.name = buf.readUtf();
        this.program = RouteProgram.readFromBuf(buf);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(name);
        program.writeToBuf(buf);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            tower.savePreset(name, program);
            tower.setActiveRoute(program);
            S2CTowerState.sendToPlayer(player, tower);
        }
    }

    @Override
    public CustomPacketPayload.Type<C2SSavePreset> type() {
        return TYPE;
    }
}
