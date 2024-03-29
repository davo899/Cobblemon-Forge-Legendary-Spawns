package com.selfdot.cobblemon.legendaryspawns;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.selfdot.cobblemon.legendaryspawns.spawnlocation.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.material.Material;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LegendarySpawner {

  private static final LegendarySpawner instance = new LegendarySpawner();
  public static LegendarySpawner getInstance() { return instance; }

  private static final int TICKS_PER_SECOND = 40;

  private int spawnCountdown;
  private SpawnLocationSelector spawnLocationSelector;
  private List<SpawnSafetyCondition> spawnSafetyConditions;

  private MinecraftServer server;
  private int spawnIntervalTicks;
  private int maximumSpawnAttempts;
  private int minimumRequiredPlayers;
  private int shinyOdds;
  private List<SpawnPool> spawnPools = new ArrayList<>();

  public boolean loadConfig() {
    final JsonObject defaultConfiguration = new JsonObject();
    defaultConfiguration.addProperty(ConfigKey.SPAWN_INTERVAL_SECONDS, 3600);
    defaultConfiguration.addProperty(ConfigKey.MINIMUM_SPAWN_DISTANCE, 32);
    defaultConfiguration.addProperty(ConfigKey.MAXIMUM_SPAWN_DISTANCE, 128);
    defaultConfiguration.addProperty(ConfigKey.MINIMUM_REQUIRED_PLAYERS, 1);
    defaultConfiguration.addProperty(ConfigKey.MAXIMUM_SPAWN_ATTEMPTS, 10);
    defaultConfiguration.addProperty(ConfigKey.SHINY_ODDS, 4096);
    defaultConfiguration.addProperty(ConfigKey.LIGHTNING_STRIKES_PER_SPAWN, 6);
    defaultConfiguration.add(ConfigKey.SPAWN_POOLS, new JsonArray());

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JsonObject configuration;
    try {
      configuration = JsonParser.parseReader(new FileReader(ConfigKey.CONFIG_LOCATION))
          .getAsJsonObject();
    } catch (FileNotFoundException e) {
      LegendarySpawnsMod.LOGGER.warn("Config not found, attempting to generate default");
      configuration = new JsonObject();
    }
    final JsonObject finalConfiguration = configuration;

    boolean rewriteConfigFile = defaultConfiguration.keySet().stream().anyMatch(k -> !finalConfiguration.has(k));
    defaultConfiguration.keySet().stream()
        .filter(k -> !finalConfiguration.has(k))
        .forEach(k -> {
          LegendarySpawnsMod.LOGGER.warn("Config key " + k + " missing, applying default value: " + defaultConfiguration.get(k));
          finalConfiguration.add(k, defaultConfiguration.get(k));
        });

    if (rewriteConfigFile) {
      try {
        Files.createDirectories(Paths.get(ConfigKey.CONFIG_LOCATION).getParent());
        FileWriter writer = new FileWriter(ConfigKey.CONFIG_LOCATION);
        gson.toJson(finalConfiguration, writer);
        writer.close();
      } catch (IOException e2) {
        LegendarySpawnsMod.LOGGER.error("Unable to generate config file");
        return false;
      }
    }
    final int minimumSpawnDistance = finalConfiguration.get(ConfigKey.MINIMUM_SPAWN_DISTANCE).getAsInt();
    this.spawnIntervalTicks = finalConfiguration.get(ConfigKey.SPAWN_INTERVAL_SECONDS).getAsInt() * TICKS_PER_SECOND;
    this.spawnCountdown = spawnIntervalTicks;
    this.maximumSpawnAttempts = finalConfiguration.get(ConfigKey.MAXIMUM_SPAWN_ATTEMPTS).getAsInt();
    this.minimumRequiredPlayers = finalConfiguration.get(ConfigKey.MINIMUM_REQUIRED_PLAYERS).getAsInt();
    this.shinyOdds = finalConfiguration.get(ConfigKey.SHINY_ODDS).getAsInt();

    spawnPools.forEach(SpawnPool::stopCaptureListener);
    spawnPools.clear();
    finalConfiguration.get(ConfigKey.SPAWN_POOLS).getAsJsonArray().iterator().forEachRemaining(
        (element) -> spawnPools.add(SpawnPool.loadSpawnPool(element.getAsJsonObject()))
    );
    spawnPools.removeIf(Objects::isNull);
    spawnPools.forEach(spawnPool -> spawnPool.startCaptureListener(server));

    LightingStriker.getInstance().setStrikeInterval(
        spawnIntervalTicks / finalConfiguration.get(ConfigKey.LIGHTNING_STRIKES_PER_SPAWN).getAsInt()
    );
    this.spawnLocationSelector = new RandomNearbySurfacePoint(
        minimumSpawnDistance, finalConfiguration.get(ConfigKey.MAXIMUM_SPAWN_DISTANCE).getAsInt()
    );
    this.spawnSafetyConditions = List.of(
        new UnsafeFloorBlocks(List.of(Material.FIRE, Material.LAVA, Material.CACTUS)),
        new AboveYLevel(60),
        new BelowYLevel(200),
        new SkyVisible(true)
    );
    LegendaryDespawner.getInstance().setMinimumDespawnDistance(minimumSpawnDistance);
    LegendaryDespawner.getInstance().setSpawnIntervalTicks(spawnIntervalTicks);
    return true;
  }

  public void setServer(MinecraftServer server) {
    this.server = server;
  }

  public void tick() {
    if (spawnCountdown > 0) spawnCountdown--;
    else {
      spawnPools.forEach(spawnPool -> spawnPool.attemptSpawn(
        server,
        spawnLocationSelector,
        spawnSafetyConditions,
        minimumRequiredPlayers,
        maximumSpawnAttempts,
        shinyOdds
      ));

      spawnCountdown = spawnIntervalTicks;
    }
    LightingStriker.getInstance().tick();
  }

}
