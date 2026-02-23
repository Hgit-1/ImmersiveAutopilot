package com.immersiveautopilot.route;

import org.jetbrains.annotations.Nullable;

public interface AutopilotRouteHolder {
    @Nullable
    RouteProgram getAutopilotPrimary();

    void setAutopilotPrimary(@Nullable RouteProgram program);

    @Nullable
    RouteProgram getAutopilotBackup();

    void setAutopilotBackup(@Nullable RouteProgram program);
}
