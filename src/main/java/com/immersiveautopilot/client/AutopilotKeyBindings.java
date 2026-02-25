package com.immersiveautopilot.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class AutopilotKeyBindings {
    public static final String CATEGORY = "key.categories.immersive_autopilot";
    public static final KeyMapping TOGGLE_AUTOPILOT = new KeyMapping(
            "key.immersive_autopilot.autopilot_toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            CATEGORY
    );
    public static final KeyMapping CYCLE_ROUTE = new KeyMapping(
            "key.immersive_autopilot.route_cycle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_APOSTROPHE,
            CATEGORY
    );

    private AutopilotKeyBindings() {
    }
}
