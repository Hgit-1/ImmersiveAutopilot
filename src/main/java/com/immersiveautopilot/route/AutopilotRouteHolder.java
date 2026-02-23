package com.immersiveautopilot.route;

import org.jetbrains.annotations.Nullable;

public interface AutopilotRouteHolder {
    @Nullable
    RouteProgram getAutopilotRoute();

    void setAutopilotRoute(@Nullable RouteProgram program);
}
