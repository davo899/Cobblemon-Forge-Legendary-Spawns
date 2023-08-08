package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.selfdot.cobblemon.legendaryspawns.spawnlocation.*;
import kotlin.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static com.selfdot.cobblemon.legendaryspawns.ChatColourUtils.formattedAnnouncement;

public class LegendarySpawner {

  private static final LegendarySpawner instance = new LegendarySpawner();
  private LegendarySpawner() {
    CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, (pokemonCapturedEvent) -> {
      Pokemon caught = pokemonCapturedEvent.component1();
      ServerPlayer player = pokemonCapturedEvent.component2();
      if (legendarySpawnList.stream().anyMatch(legendarySpawn -> legendarySpawn.species == caught.getSpecies())) {
        server.getPlayerList().broadcastSystemMessage(
            formattedAnnouncement(legendaryCaptureAnnouncement, caught, player),
            false
        );
      } else if (
          ultraBeastSpawnList.stream().anyMatch(legendarySpawn -> legendarySpawn.species == caught.getSpecies())
      ) {
        server.getPlayerList().broadcastSystemMessage(
            formattedAnnouncement(ultraBeastCaptureAnnouncement, caught, player),
            false
        );
      }
      return Unit.INSTANCE;
    });
  }
  public static LegendarySpawner getInstance() { return instance; }

  private static final int TICKS_PER_SECOND = 40;

  private int spawnCountdown;
  private SpawnLocationSelector spawnLocationSelector;
  private List<SpawnSafetyCondition> spawnSafetyConditions;

  private MinecraftServer server;
  private List<LegendarySpawn> legendarySpawnList;
  private List<LegendarySpawn> ultraBeastSpawnList;
  private int spawnIntervalTicks;
  private int maximumSpawnAttempts;
  private int minimumRequiredPlayers;
  private int shinyOdds;
  private String legendarySpawnAnnouncement;
  private String ultraBeastSpawnAnnouncement;
  private String legendaryCaptureAnnouncement;
  private String ultraBeastCaptureAnnouncement;
  private float legendarySpawnChance;
  private float ultraBeastSpawnChance;

  public boolean loadConfig() {
    legendarySpawnList = loadSpawnList("config/legendaryspawns.txt");
    if (legendarySpawnList == null) return false;
    ultraBeastSpawnList = loadSpawnList("config/ultrabeastspawns.txt");
    if (ultraBeastSpawnList == null) return false;

    final JsonObject defaultConfiguration = new JsonObject();
    defaultConfiguration.addProperty(ConfigKey.SPAWN_INTERVAL_SECONDS, 3600);
    defaultConfiguration.addProperty(ConfigKey.MINIMUM_SPAWN_DISTANCE, 32);
    defaultConfiguration.addProperty(ConfigKey.MAXIMUM_SPAWN_DISTANCE, 128);
    defaultConfiguration.addProperty(ConfigKey.MINIMUM_REQUIRED_PLAYERS, 1);
    defaultConfiguration.addProperty(ConfigKey.MAXIMUM_SPAWN_ATTEMPTS, 10);
    defaultConfiguration.addProperty(ConfigKey.SHINY_ODDS, 4096);
    defaultConfiguration.addProperty(ConfigKey.LIGHTNING_STRIKES_PER_SPAWN, 6);
    defaultConfiguration.add(ConfigKey.SPAWN_GROUPS, new JsonArray());
    defaultConfiguration.addProperty(ConfigKey.LEGENDARY_SPAWN_ANNOUNCEMENT, "&cA &eLegendary &3%legendary% &chas spawned nearby &3%player%&c!");
    defaultConfiguration.addProperty(ConfigKey.ULTRA_BEAST_SPAWN_ANNOUNCEMENT, "&cAn &dUltra Beast &3%ultrabeast% &chas spawned nearby &3%player%&c!");
    defaultConfiguration.addProperty(ConfigKey.LEGENDARY_CAPTURE_ANNOUNCEMENT, "&cThe &eLegendary &3%legendary% &chas been captured by &3%player%&c!");
    defaultConfiguration.addProperty(ConfigKey.ULTRA_BEAST_CAPTURE_ANNOUNCEMENT, "&cThe &dUltra Beast &3%ultrabeast% &chas been captured by &3%player%&c!");
    defaultConfiguration.addProperty(ConfigKey.LEGENDARY_SPAWN_CHANCE, 1f);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JsonObject configuration;
    try {
      configuration = JsonParser.parseReader(new FileReader("config/legendaryspawnsConfig.json"))
          .getAsJsonObject();
    } catch (FileNotFoundException e) {
      LogUtils.getLogger().warn("Config not found, attempting to generate default");
      configuration = new JsonObject();
    }
    final JsonObject finalConfiguration = configuration;

    boolean rewriteConfigFile = defaultConfiguration.keySet().stream().anyMatch(k -> !finalConfiguration.has(k));
    defaultConfiguration.keySet().stream()
        .filter(k -> !finalConfiguration.has(k))
        .forEach(k -> {
          LogUtils.getLogger().warn("Config key " + k + " missing, applying default value: " + defaultConfiguration.get(k));
          finalConfiguration.add(k, defaultConfiguration.get(k));
        });

    if (rewriteConfigFile) {
      try {
        FileWriter writer = new FileWriter("config/legendaryspawnsConfig.json");
        gson.toJson(finalConfiguration, writer);
        writer.close();
      } catch (IOException e2) {
        LogUtils.getLogger().error("Unable to generate config file");
        return false;
      }
    }
    final int minimumSpawnDistance = finalConfiguration.get(ConfigKey.MINIMUM_SPAWN_DISTANCE).getAsInt();
    this.spawnIntervalTicks = finalConfiguration.get(ConfigKey.SPAWN_INTERVAL_SECONDS).getAsInt() * TICKS_PER_SECOND;
    this.maximumSpawnAttempts = finalConfiguration.get(ConfigKey.MAXIMUM_SPAWN_ATTEMPTS).getAsInt();
    this.minimumRequiredPlayers = finalConfiguration.get(ConfigKey.MINIMUM_REQUIRED_PLAYERS).getAsInt();
    this.shinyOdds = finalConfiguration.get(ConfigKey.SHINY_ODDS).getAsInt();
    this.legendarySpawnAnnouncement = finalConfiguration.get(ConfigKey.LEGENDARY_SPAWN_ANNOUNCEMENT).getAsString();
    this.ultraBeastSpawnAnnouncement = finalConfiguration.get(ConfigKey.ULTRA_BEAST_SPAWN_ANNOUNCEMENT).getAsString();
    this.legendaryCaptureAnnouncement = finalConfiguration.get(ConfigKey.LEGENDARY_CAPTURE_ANNOUNCEMENT).getAsString();
    this.ultraBeastCaptureAnnouncement = finalConfiguration.get(ConfigKey.ULTRA_BEAST_CAPTURE_ANNOUNCEMENT).getAsString();
    this.legendarySpawnChance = finalConfiguration.get(ConfigKey.LEGENDARY_SPAWN_CHANCE).getAsFloat();
    this.ultraBeastSpawnChance = finalConfiguration.get(ConfigKey.ULTRA_BEAST_SPAWN_CHANCE).getAsFloat();
    this.spawnCountdown = spawnIntervalTicks;
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


  @Nullable
  private List<LegendarySpawn> loadSpawnList(String filename) {
    List<LegendarySpawn> spawnList = new ArrayList<>();
    try {
      File legendarySpawnListFile = new File(filename);
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

        spawnList.add(new LegendarySpawn(species, level));
      }
      configReader.close();

      if (spawnList.isEmpty()) LogUtils.getLogger().warn("Spawn list " + filename + " is empty");
      return spawnList;

    } catch (FileNotFoundException e) {
      LogUtils.getLogger().warn("Spawn list " + filename + " not found");
      return null;
    }
  }

  public void setServer(MinecraftServer server) {
    this.server = server;
  }

  private float scaledChance(float base) {
    return base + (0.02f * (server.getPlayerCount() - minimumRequiredPlayers));
  }

  public void tick() {
    if (spawnCountdown > 0) spawnCountdown--;
    else {
      if (Math.random() < scaledChance(legendarySpawnChance)) {
        spawnFromGroup(legendarySpawnList, legendarySpawnAnnouncement);
      }
      if (Math.random() < scaledChance(ultraBeastSpawnChance)) {
        spawnFromGroup(ultraBeastSpawnList, ultraBeastSpawnAnnouncement);
      }
      spawnCountdown = spawnIntervalTicks;
    }
    LightingStriker.getInstance().tick();
  }

  private void logSkippingSpawn(String reason) {
    LogUtils.getLogger().warn("Skipping spawn: " + reason);
  }

  private void spawnFromGroup(List<LegendarySpawn> group, String spawnAnnouncement) {
    List<ServerPlayer> players = server.getPlayerList().getPlayers();
    if (players.size() < minimumRequiredPlayers) return;

    Optional<LegendarySpawn> chosenLegendaryOpt = group.stream()
        .skip((int) (group.size() * Math.random()))
        .findFirst();

    LegendarySpawn chosenLegendary;
    if (chosenLegendaryOpt.isPresent())
      chosenLegendary = chosenLegendaryOpt.get();
    else {
      logSkippingSpawn("Empty spawn list");
      return;
    }

    int attemptedSpawns = 0;
    Level spawnLevel;
    Vec3 spawnPos;
    ServerPlayer chosenPlayer;

    while (true) {
      if (++attemptedSpawns > maximumSpawnAttempts) {
        logSkippingSpawn("Could not find safe spawn location after " + maximumSpawnAttempts + " attempts");
        return;
      }

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
    legendary.initializeMoveset(true);
    if (Math.random() < (1d / shinyOdds)) legendary.setShiny(true);

    PokemonEntity pokemonEntity = new PokemonEntity(spawnLevel, legendary, CobblemonEntities.POKEMON.get());
    pokemonEntity.setDespawner(LegendaryDespawner.getInstance());
    pokemonEntity.setPos(spawnPos);
    LightingStriker.getInstance().addTracked(pokemonEntity);
    spawnLevel.getChunkAt(new BlockPos(spawnPos));
    spawnLevel.addFreshEntity(pokemonEntity);

    server.getPlayerList().broadcastSystemMessage(
        formattedAnnouncement(spawnAnnouncement, pokemonEntity.getPokemon(), chosenPlayer), false
    );
  }

}
