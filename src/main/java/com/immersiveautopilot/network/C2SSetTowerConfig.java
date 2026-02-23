package com.immersiveautopilot.network;

import com.immersiveautopilot.blockentity.TowerBlockEntity;
import immersive_aircraft.cobalt.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class C2SSetTowerConfig extends Message {
    public static final CustomPacketPayload.Type<C2SSetTowerConfig> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("immersive_autopilot", "set_tower_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetTowerConfig> STREAM_CODEC = StreamCodec.ofMember(C2SSetTowerConfig::encode, C2SSetTowerConfig::new);

    private final BlockPos pos;
    private final String towerName;
    private final String autoRequestText;
    private final String enterText;
    private final String exitText;
    private final boolean targetAllInRange;

    public C2SSetTowerConfig(BlockPos pos, String towerName, String autoRequestText, String enterText, String exitText, boolean targetAllInRange) {
        this.pos = pos;
        this.towerName = towerName;
        this.autoRequestText = autoRequestText;
        this.enterText = enterText;
        this.exitText = exitText;
        this.targetAllInRange = targetAllInRange;
    }

    public C2SSetTowerConfig(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.towerName = buf.readUtf();
        this.autoRequestText = buf.readUtf();
        this.enterText = buf.readUtf();
        this.exitText = buf.readUtf();
        this.targetAllInRange = buf.readBoolean();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(towerName == null ? "" : towerName);
        buf.writeUtf(autoRequestText == null ? "" : autoRequestText);
        buf.writeUtf(enterText == null ? "" : enterText);
        buf.writeUtf(exitText == null ? "" : exitText);
        buf.writeBoolean(targetAllInRange);
    }

    @Override
    public void receiveServer(ServerPlayer player) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            tower.setTowerName(towerName);
            tower.setAutoRequestText(autoRequestText);
            tower.setEnterText(enterText);
            tower.setExitText(exitText);
            tower.setTargetAllInRange(targetAllInRange);
            S2CTowerState.sendToPlayer(player, tower);
        }
    }

    @Override
    public CustomPacketPayload.Type<C2SSetTowerConfig> type() {
        return TYPE;
    }
}
