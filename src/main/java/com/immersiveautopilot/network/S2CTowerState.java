package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import com.immersiveautopilot.client.ClientCache;
import com.immersiveautopilot.data.TowerState;
import com.immersiveautopilot.route.RouteProgram;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class S2CTowerState extends Message {
    public static final CustomPacketPayload.Type<S2CTowerState> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "tower_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CTowerState> STREAM_CODEC = StreamCodec.ofMember(S2CTowerState::encode, S2CTowerState::new);

    private final TowerState state;

    public S2CTowerState(TowerState state) {
        this.state = state;
    }

    public S2CTowerState(RegistryFriendlyByteBuf buf) {
        this.state = TowerState.readFromBuf(buf);
    }

    public static void sendToPlayer(ServerPlayer player, TowerBlockEntity tower) {
        BlockPos pos = tower.getBlockPos();
        Level level = player.level();
        UUID bound = tower.getBoundAircraft();
        String boundName = "";
        if (bound != null && level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(bound);
            if (entity != null) {
                boundName = entity.getDisplayName().getString();
            }
        }
        RouteProgram active = tower.getActiveRoute();
        java.util.List<String> presets = new java.util.ArrayList<>(tower.getPresets().keySet());
        presets.sort(String::compareToIgnoreCase);
        TowerState state = new TowerState(
                pos,
                tower.getScanRange(),
                bound,
                boundName,
                active,
                tower.getTowerName(),
                tower.getAutoRequestText(),
                tower.getEnterText(),
                tower.getExitText(),
                tower.isTargetAllInRange(),
                tower.isPowered(),
                tower.hasAutoSupportNearby(),
                presets
        );
        immersive_aircraft.cobalt.network.NetworkHandler.sendToPlayer(new S2CTowerState(state), player);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        state.writeToBuf(buf);
    }

    @Override
    public void receiveClient() {
        ClientCache.setTowerState(state);
    }

    @Override
    public CustomPacketPayload.Type<S2CTowerState> type() {
        return TYPE;
    }
}
