package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(LegendarySpawnsMod.MODID)
public class LegendarySpawnsMod {

  // Define mod id in a common place for everything to reference
  public static final String MODID = "legendaryspawns";
  // Directly reference a slf4j logger
  private static final Logger LOGGER = LogUtils.getLogger();

  private LegendarySpawner legendarySpawner;

  public LegendarySpawnsMod() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    // Register the commonSetup method for modloading
    modEventBus.addListener(this::commonSetup);

    // Register ourselves for server and other game events we are interested in
    MinecraftForge.EVENT_BUS.register(this);
  }

  private void warnBadLegendarySpawnListLine(String line) {
    LOGGER.warn("Legendary Spawns could not interpret line in config/legendaryspawns.txt: " + line);
  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    LOGGER.info("Setting up Legendary Spawns");
  }

  @SubscribeEvent
  public void onServerStart(ServerStartingEvent event) {
    final JsonObject defaultConfiguration = new JsonObject();
    defaultConfiguration.addProperty(ConfigKey.SPAWN_INTERVAL_SECONDS, 3600);
    defaultConfiguration.addProperty(ConfigKey.MINIMUM_SPAWN_DISTANCE, 32);
    defaultConfiguration.addProperty(ConfigKey.MAXIMUM_SPAWN_DISTANCE, 128);
    defaultConfiguration.addProperty(ConfigKey.MINIMUM_REQUIRED_PLAYERS, 1);
    defaultConfiguration.addProperty(ConfigKey.MAXIMUM_SPAWN_ATTEMPTS, 5);
    defaultConfiguration.addProperty(ConfigKey.SHINY_ODDS, 4096);
    defaultConfiguration.addProperty(ConfigKey.LIGHTNING_STRIKES_PER_SPAWN, 6);

    Gson gson = new Gson();
    JsonObject configuration;
    try {
      JsonParser parser = new JsonParser();
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
        LOGGER.warn("Legendary Spawns missing some config options, generating defaults");
        FileWriter writer = new FileWriter("config/legendaryspawnsConfig.json");
        gson.toJson(finalConfiguration, writer);
        writer.close();
      } catch (IOException e2) {
        LOGGER.error("Legendary Spawns unable to generate config file");
        return;
      }
    }

    legendarySpawner = new LegendarySpawner(
        event.getServer(),
        loadLegendarySpawnList(),
        finalConfiguration.get(ConfigKey.SPAWN_INTERVAL_SECONDS).getAsInt(),
        finalConfiguration.get(ConfigKey.MINIMUM_SPAWN_DISTANCE).getAsInt(),
        finalConfiguration.get(ConfigKey.MAXIMUM_SPAWN_DISTANCE).getAsInt(),
        finalConfiguration.get(ConfigKey.MAXIMUM_SPAWN_ATTEMPTS).getAsInt(),
        finalConfiguration.get(ConfigKey.MINIMUM_REQUIRED_PLAYERS).getAsInt(),
        finalConfiguration.get(ConfigKey.SHINY_ODDS).getAsInt(),
        finalConfiguration.get(ConfigKey.LIGHTNING_STRIKES_PER_SPAWN).getAsInt()
    );
  }

  @SubscribeEvent
  public void tick(TickEvent.ServerTickEvent event) {
    legendarySpawner.tick();
  }

  private List<LegendarySpawn> loadLegendarySpawnList() {
    final List<LegendarySpawn> legendarySpawnList = new ArrayList<>();
    try {
      File legendarySpawnListFile = new File("config/legendaryspawns.txt");
      Scanner configReader = new Scanner(legendarySpawnListFile);
      while (configReader.hasNextLine()) {
        String line = configReader.nextLine();
        String[] parts = line.split(",");
        if (parts.length != 2) {
          warnBadLegendarySpawnListLine(line);
          continue;
        }

        Species species = PokemonSpecies.INSTANCE.getByName(parts[0]);
        if (species == null) {
          warnBadLegendarySpawnListLine(line);
          continue;
        }

        int level = 0;
        try {
          level = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
          warnBadLegendarySpawnListLine(line);
          continue;
        }

        legendarySpawnList.add(new LegendarySpawn(species, level));
      }
      configReader.close();
    } catch (FileNotFoundException e) {
      LOGGER.error("Legendary Spawns missing legendary spawn list file: config/legendaryspawns.txt");
    }

    return legendarySpawnList;
  }
}
