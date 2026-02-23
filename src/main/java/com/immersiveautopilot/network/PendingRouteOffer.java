package com.immersiveautopilot.network;

import com.immersiveautopilot.route.RouteEntry;

import java.util.List;
import java.util.UUID;

public record PendingRouteOffer(UUID pilotUuid, UUID operatorUuid, int vehicleId, List<RouteEntry> entries, long expiresAt) {
}
