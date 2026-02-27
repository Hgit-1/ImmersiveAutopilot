package com.immersiveautopilot.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;

public final class ClientSideExecutor {
    private ClientSideExecutor() {
    }

    public static void run(String methodName, Class<?> paramType, Object param) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        try {
            Class<?> handler = Class.forName("com.immersiveautopilot.client.ClientPacketHandlers");
            Method method = handler.getMethod(methodName, paramType);
            method.invoke(null, param);
        } catch (Exception ignored) {
            // Client-side only; ignore if not present or fails to invoke.
        }
    }
}
