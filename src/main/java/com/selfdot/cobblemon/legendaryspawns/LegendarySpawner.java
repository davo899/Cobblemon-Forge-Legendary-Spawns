package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.selfdot.cobblemon.legendaryspawns.spawnlocation.RandomNearbyPoint;
import com.selfdot.cobblemon.legendaryspawns.spawnlocation.SpawnLocationSelector;
import com.selfdot.cobblemon.legendaryspawns.spawnlocation.SpawnSafetyCondition;
import com.selfdot.cobblemon.legendaryspawns.spawnlocation.UnsafeFloorBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegendarySpawner {

  private static final LegendarySpawner instance = new LegendarySpawner();
  private LegendarySpawner() { }
  public static LegendarySpawner getInstance() { return instance; }

  private static final int TICKS_PER_SECOND = 40;

  private int spawnCountdown;
  private SpawnLocationSelector spawnLocationSelector;
  private List<SpawnSafetyCondition> spawnSafetyConditions;

  private MinecraftServer server;
  private List<LegendarySpawn> legendarySpawnList;
  private int spawnIntervalTicks;
  private int maximumSpawnAttempts;
  private int minimumRequiredPlayers;
  private int shinyOdds;
  private String legendarySpawnAnnouncement;

  public boolean loadConfig() {
    legendarySpawnList = new ArrayList<>();
    try {
      File legendarySpawnListFile = new File("config/legendaryspawns.txt");
      Scanner configReader = new Scanner(legendarySpawnListFile);
      while (configReader.hasNextLine()) {
        String line = configReader.nextLine();
        String[] parts = line.split(",");
        if (parts.length != 2) continue;

        Species species = PokemonSpecies.INSTANCE.getByName(parts[0]);
        if (species == null) continue;

        int level;
        try {
          level = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
          continue;
        }

        legendarySpawnList.add(new LegendarySpawn(species, level));
      }
      configReader.close();
    } catch (FileNotFoundException e) {
      return false;
    }

    final JsonObject defaultConfiguration = new JsonObject();
    defaultConfiguration.addProperty(ConfigKey.SPAWN_INTERVAL_SECONDS, 3600);
    defaultConfiguration.addProperty(ConfigKey.MINIMUM_SPAWN_DISTANCE, 32);
    defaultConfiguration.addProperty(ConfigKey.MAXIMUM_SPAWN_DISTANCE, 128);
    defaultConfiguration.addProperty(ConfigKey.MINIMUM_REQUIRED_PLAYERS, 1);
    defaultConfiguration.addProperty(ConfigKey.MAXIMUM_SPAWN_ATTEMPTS, 5);
    defaultConfiguration.addProperty(ConfigKey.SHINY_ODDS, 4096);
    defaultConfiguration.addProperty(ConfigKey.LIGHTNING_STRIKES_PER_SPAWN, 6);
    defaultConfiguration.addProperty(ConfigKey.LEGENDARY_SPAWN_ANNOUNCEMENT, "&cA &eLegendary &3%legendary% &chas spawned nearby &3%player%&c!");

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JsonObject configuration;
    try {
      configuration = JsonParser.parseReader(new FileReader("config/legendaryspawnsConfig.json"))
          .getAsJsonObject();
    } catch (FileNotFoundException e) {
      configuration = new JsonObject();
    }
    final JsonObject finalConfiguration = configuration;

    boolean rewriteConfigFile = defaultConfiguration.keySet().stream().anyMatch(k -> !finalConfiguration.has(k));
    defaultConfiguration.keySet().stream()
        .filter(k -> !finalConfiguration.has(k))
        .forEach(k -> finalConfiguration.add(k, defaultConfiguration.get(k)));

    if (rewriteConfigFile) {
      try {
        FileWriter writer = new FileWriter("config/legendaryspawnsConfig.json");
        gson.toJson(finalConfiguration, writer);
        writer.close();
      } catch (IOException e2) {
        return false;
      }
    }
    final int minimumSpawnDistance = finalConfiguration.get(ConfigKey.MINIMUM_SPAWN_DISTANCE).getAsInt();
    this.spawnIntervalTicks = finalConfiguration.get(ConfigKey.SPAWN_INTERVAL_SECONDS).getAsInt() * TICKS_PER_SECOND;
    this.maximumSpawnAttempts = finalConfiguration.get(ConfigKey.MAXIMUM_SPAWN_ATTEMPTS).getAsInt();
    this.minimumRequiredPlayers = finalConfiguration.get(ConfigKey.MINIMUM_REQUIRED_PLAYERS).getAsInt();
    this.shinyOdds = finalConfiguration.get(ConfigKey.SHINY_ODDS).getAsInt();
    this.legendarySpawnAnnouncement = finalConfiguration.get(ConfigKey.LEGENDARY_SPAWN_ANNOUNCEMENT).getAsString();
    this.spawnCountdown = spawnIntervalTicks;
    LightingStriker.getInstance().setStrikeInterval(
        spawnIntervalTicks / finalConfiguration.get(ConfigKey.LIGHTNING_STRIKES_PER_SPAWN).getAsInt()
    );
    this.spawnLocationSelector = new RandomNearbyPoint(
        minimumSpawnDistance, finalConfiguration.get(ConfigKey.MAXIMUM_SPAWN_DISTANCE).getAsInt()
    );
    this.spawnSafetyConditions = List.of(new UnsafeFloorBlocks(List.of(Material.FIRE, Material.LAVA, Material.CACTUS)));
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
      spawnLegendary();
      spawnCountdown = spawnIntervalTicks;
    }
    LightingStriker.getInstance().tick();
  }

  private void spawnLegendary() {
    List<ServerPlayer> players = server.getPlayerList().getPlayers();
    if (players.size() < minimumRequiredPlayers) return;

    Optional<LegendarySpawn> chosenLegendaryOpt = legendarySpawnList.stream()
        .skip((int) (legendarySpawnList.size() * Math.random()))
        .findFirst();

    LegendarySpawn chosenLegendary;
    if (chosenLegendaryOpt.isPresent())
      chosenLegendary = chosenLegendaryOpt.get();
    else return;

    int attemptedSpawns = 0;
    Level spawnLevel;
    Vec3 spawnPos;
    ServerPlayer chosenPlayer;

    while (true) {
      if (++attemptedSpawns > maximumSpawnAttempts) {
        LogUtils.getLogger().info("Skipping Legendary spawn: Could not find safe spawn location after " +
            maximumSpawnAttempts + " attempts");
        return;
      }
      // Choose random player
      Optional<ServerPlayer> chosenPlayerOpt = players.stream()
          .filter(player -> player.level.dimension() == Level.OVERWORLD)
          .skip((int) (players.size() * Math.random()))
          .findFirst();

      if (chosenPlayerOpt.isPresent()) {
        chosenPlayer = chosenPlayerOpt.get();
        final Level chosenPlayerSpawnLevel = chosenPlayer.level;
        final Vec3 spawnLocation = spawnLocationSelector.getSpawnLocation(
            chosenPlayer.level, chosenPlayer.getPosition(0f)
        );
        if (spawnLocation == null) continue;
        BlockPos finalSpawnPos = new BlockPos(spawnLocation);
        if (spawnSafetyConditions.stream().anyMatch(condition -> !condition.isSafe(chosenPlayerSpawnLevel, finalSpawnPos))) continue;
        spawnPos = spawnLocation;
        spawnLevel = chosenPlayerSpawnLevel;
        break;
      }

    }

    Pokemon legendary = new Pokemon();
    legendary.setSpecies(chosenLegendary.species);
    legendary.setLevel(chosenLegendary.level);
    if (Math.random() < (1d / shinyOdds)) legendary.setShiny(true);

    PokemonEntity pokemonEntity = new PokemonEntity(
        spawnLevel,
        legendary,
        CobblemonEntities.POKEMON.get()
    );
    pokemonEntity.setDespawner(LegendaryDespawner.getInstance());
    pokemonEntity.setPos(spawnPos);
    spawnLevel.addFreshEntity(pokemonEntity);

    LightingStriker.getInstance().setTracked(pokemonEntity);

    server.getPlayerList().broadcastSystemMessage(
        Component.literal(ChatColourUtils.format(legendarySpawnAnnouncement)
            .replaceAll("%legendary%", pokemonEntity.getPokemon().getSpecies().getTranslatedName().getString())
            .replaceAll("%player%", chosenPlayer.getDisplayName().getString())
        ),
        false
    );
  }
}
