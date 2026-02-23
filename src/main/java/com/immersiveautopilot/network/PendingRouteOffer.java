package com.immersiveautopilot.network;

import com.immersiveautopilot.route.RouteProgram;

import java.util.UUID;

public record PendingRouteOffer(UUID pilotUuid, UUID operatorUuid, int vehicleId, RouteProgram program, long expiresAt) {
}
