package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class C2SRequestAircraftList extends Message {
    public static final CustomPacketPayload.Type<C2SRequestAircraftList> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "request_aircraft_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestAircraftList> STREAM_CODEC = StreamCodec.ofMember(C2SRequestAircraftList::encode, C2SRequestAircraftList::new);

    private final BlockPos pos;

    public C2SRequestAircraftList(BlockPos pos) {
        this.pos = pos;
    }

    public C2SRequestAircraftList(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        Level level = player.level();
        if (!isNear(player, pos)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            S2CAircraftList.sendToPlayer(player, pos, tower.getScanRange());
        }
    }

    private boolean isNear(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public CustomPacketPayload.Type<C2SRequestAircraftList> type() {
        return TYPE;
    }
}
