package com.immersiveautopilot.client;

import com.immersiveautopilot.network.C2SPilotRouteDecision;
import com.immersiveautopilot.route.RouteEntry;
import com.immersiveautopilot.route.RouteProgram;
import immersive_aircraft.cobalt.network.NetworkHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoRouteClient {
    public record Entry(String routeName, String label) {
    }

    private static final Map<Integer, List<Entry>> ROUTES = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> INDICES = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> ACCEPTED = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> LAST_REQUEST = new ConcurrentHashMap<>();
    private static final Map<Integer, List<Entry>> PENDING = new ConcurrentHashMap<>();
    private static final Map<Integer, List<OfferCandidate>> OFFERS = new ConcurrentHashMap<>();

    public record OfferCandidate(String operatorName, RouteEntry entry) {
    }

    private AutoRouteClient() {
    }

    public static void setRoutes(int vehicleId, List<Entry> entries) {
        ROUTES.put(vehicleId, entries == null ? List.of() : new ArrayList<>(entries));
        INDICES.put(vehicleId, 0);
        ACCEPTED.put(vehicleId, false);
        PENDING.remove(vehicleId);
        OFFERS.remove(vehicleId);
    }

    public static void requestRoutes(int vehicleId) {
        long now = System.currentTimeMillis();
        long last = LAST_REQUEST.getOrDefault(vehicleId, 0L);
        if (now - last < 2000L) {
            return;
        }
        LAST_REQUEST.put(vehicleId, now);
        immersive_aircraft.cobalt.network.NetworkHandler.sendToServer(
                new com.immersiveautopilot.network.C2SRequestAutoRoutes(vehicleId));
    }

    public static List<Entry> getRoutes(int vehicleId) {
        return ROUTES.getOrDefault(vehicleId, Collections.emptyList());
    }

    public static int getIndex(int vehicleId) {
        return INDICES.getOrDefault(vehicleId, 0);
    }

    public static boolean tryAutoAccept(int vehicleId, List<RouteEntry> offers) {
        return false;
    }

    public static void onAirspaceEnter(int vehicleId) {
        ACCEPTED.put(vehicleId, false);
        requestRoutes(vehicleId);
    }

    public static void onAirspaceExit(int vehicleId) {
        ACCEPTED.put(vehicleId, false);
        requestRoutes(vehicleId);
    }

    public static List<Entry> getPending(int vehicleId) {
        return PENDING.getOrDefault(vehicleId, Collections.emptyList());
    }

    public static void acceptPending(int vehicleId, String routeName, String operatorName) {
        if (routeName == null || routeName.isBlank()) {
            return;
        }
        List<Entry> entries = ROUTES.get(vehicleId);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        int index = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (routeName.equals(entries.get(i).routeName())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        List<OfferCandidate> offers = OFFERS.getOrDefault(vehicleId, Collections.emptyList());
        for (OfferCandidate candidate : offers) {
            if (routeName.equals(candidate.entry().name())
                    && (operatorName == null || operatorName.isBlank() || operatorName.equals(candidate.operatorName()))) {
                acceptOffer(vehicleId, candidate, index);
                PENDING.remove(vehicleId);
                return;
            }
        }
    }

    public static void registerOffer(int vehicleId, String operatorName, List<RouteEntry> offers) {
        List<OfferCandidate> list = new ArrayList<>(OFFERS.getOrDefault(vehicleId, List.of()));
        for (RouteEntry entry : offers) {
            list.add(new OfferCandidate(operatorName == null ? "" : operatorName, entry));
        }
        OFFERS.put(vehicleId, list);
        evaluateOffers(vehicleId);
    }

    public static List<OfferCandidate> getOfferCandidates(int vehicleId) {
        return OFFERS.getOrDefault(vehicleId, Collections.emptyList());
    }

    public static boolean isAccepted(int vehicleId) {
        return Boolean.TRUE.equals(ACCEPTED.get(vehicleId));
    }

    private static void evaluateOffers(int vehicleId) {
        List<Entry> entries = ROUTES.get(vehicleId);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        List<OfferCandidate> offers = OFFERS.getOrDefault(vehicleId, Collections.emptyList());
        if (offers.isEmpty()) {
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            String desired = entries.get(i).routeName();
            if (desired == null || desired.isBlank()) {
                continue;
            }
            List<OfferCandidate> matches = new ArrayList<>();
            for (OfferCandidate candidate : offers) {
                if (desired.equals(candidate.entry().name())) {
                    matches.add(candidate);
                }
            }
            if (matches.isEmpty()) {
                continue;
            }
            if (matches.size() == 1) {
                acceptOffer(vehicleId, matches.get(0), i);
                return;
            }
            List<Entry> pending = new ArrayList<>();
            for (OfferCandidate match : matches) {
                pending.add(new Entry(match.entry().name(), match.operatorName()));
            }
            PENDING.put(vehicleId, pending);
            ClientRouteGuidance.requestRouteChoice(vehicleId, pending);
            return;
        }
    }

    private static void acceptOffer(int vehicleId, OfferCandidate candidate, int index) {
        RouteEntry entry = candidate.entry();
        INDICES.put(vehicleId, index);

        RouteProgram current = ClientRouteCache.getPrimary(vehicleId);
        RouteProgram backup = current != null && current != entry.program() ? current : null;
        ClientRouteCache.setRoutes(vehicleId, entry.program(), backup);

        String label = ROUTES.getOrDefault(vehicleId, List.of()).get(index).label();
        ClientRouteGuidance.acceptRoutes(vehicleId, entry.program(), backup, label == null ? "" : label, "");
        NetworkHandler.sendToServer(new C2SPilotRouteDecision(vehicleId, true, entry.name(), ""));
        ACCEPTED.put(vehicleId, true);
        OFFERS.remove(vehicleId);
    }
}
