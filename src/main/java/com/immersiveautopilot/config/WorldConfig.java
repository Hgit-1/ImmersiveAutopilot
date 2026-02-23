package com.immersiveautopilot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorldConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static WorldConfigData cached;
    private static Path cachedPath;

    private WorldConfig() {
    }

    public static WorldConfigData get(ServerLevel level) {
        Path path = level.getServer().getWorldPath(LevelResource.ROOT).resolve("immersive_autopilot").resolve("config.json");
        if (cached != null && path.equals(cachedPath)) {
            return cached;
        }
        cachedPath = path;
        cached = load(path);
        return cached;
    }

    private static WorldConfigData load(Path path) {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                WorldConfigData data = GSON.fromJson(reader, WorldConfigData.class);
                if (data != null) {
                    return data;
                }
            } catch (IOException ignored) {
            }
        }
        WorldConfigData data = new WorldConfigData();
        save(path, data);
        return data;
    }

    private static void save(Path path, WorldConfigData data) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }
}
