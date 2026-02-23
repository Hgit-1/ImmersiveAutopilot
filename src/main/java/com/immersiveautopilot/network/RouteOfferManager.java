package com.immersiveautopilot.network;

import com.immersiveautopilot.route.RouteEntry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public final class RouteOfferManager {
    private static final Map<UUID, PendingRouteOffer> OFFERS = new ConcurrentHashMap<>();
    private static final int OFFER_LIFETIME_TICKS = 20 * 30;

    private RouteOfferManager() {
    }

    public static void createOffer(UUID pilotUuid, UUID operatorUuid, int vehicleId, List<RouteEntry> entries, long currentTick) {
        long expiresAt = currentTick + OFFER_LIFETIME_TICKS;
        OFFERS.put(pilotUuid, new PendingRouteOffer(pilotUuid, operatorUuid, vehicleId, entries, expiresAt));
    }

    public static PendingRouteOffer getOffer(UUID pilotUuid) {
        return OFFERS.get(pilotUuid);
    }

    public static PendingRouteOffer consumeOffer(UUID pilotUuid) {
        return OFFERS.remove(pilotUuid);
    }

    public static boolean isExpired(PendingRouteOffer offer, long currentTick) {
        return offer.expiresAt() < currentTick;
    }
}
