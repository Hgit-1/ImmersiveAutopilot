package com.immersiveautopilot.network;

import com.immersiveautopilot.ImmersiveAutopilot;
import immersive_aircraft.cobalt.network.NetworkHandler;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register() {
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SRequestAircraftList.TYPE, C2SRequestAircraftList.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, S2CAircraftList.TYPE, S2CAircraftList.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SRequestRadarAircraftList.TYPE, C2SRequestRadarAircraftList.STREAM_CODEC);

        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SSendRouteToAircraft.TYPE, C2SSendRouteToAircraft.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, S2CRouteOfferToPilot.TYPE, S2CRouteOfferToPilot.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SPilotRouteDecision.TYPE, C2SPilotRouteDecision.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, S2CRouteResultToOperator.TYPE, S2CRouteResultToOperator.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, S2CAirspaceState.TYPE, S2CAirspaceState.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SRequestAutoRoutes.TYPE, C2SRequestAutoRoutes.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SSetAutoRoutes.TYPE, C2SSetAutoRoutes.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, S2CAutoRoutes.TYPE, S2CAutoRoutes.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SToggleAutopilot.TYPE, C2SToggleAutopilot.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, S2CAutopilotState.TYPE, S2CAutopilotState.STREAM_CODEC);

        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SRequestTowerState.TYPE, C2SRequestTowerState.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, S2CTowerState.TYPE, S2CTowerState.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SSetTowerRange.TYPE, C2SSetTowerRange.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SBindAircraft.TYPE, C2SBindAircraft.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SUnbindAircraft.TYPE, C2SUnbindAircraft.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SSavePreset.TYPE, C2SSavePreset.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SLoadPreset.TYPE, C2SLoadPreset.STREAM_CODEC);
        NetworkHandler.registerMessage(ImmersiveAutopilot.MOD_ID, C2SSetTowerConfig.TYPE, C2SSetTowerConfig.STREAM_CODEC);
    }
}
