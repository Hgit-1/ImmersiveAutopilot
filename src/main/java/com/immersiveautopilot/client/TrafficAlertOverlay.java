package com.immersiveautopilot.client;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;

public final class TrafficAlertOverlay {
    private static final long ALERT_DURATION_MS = 3000L;
    private static long alertUntilMs = 0L;
    private static Component alertText = Component.empty();

    private TrafficAlertOverlay() {
    }

    public static void pushAlert(Component text) {
        if (text == null) {
            return;
        }
        alertText = text;
        alertUntilMs = Util.getMillis() + ALERT_DURATION_MS;
    }

    public static Component getActiveAlert() {
        if (Util.getMillis() > alertUntilMs) {
            return null;
        }
        return alertText;
    }
}
