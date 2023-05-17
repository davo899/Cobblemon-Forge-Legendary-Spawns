package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
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

import java.io.File;
import java.io.FileNotFoundException;
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

    legendarySpawner = new LegendarySpawner(event.getServer(), legendarySpawnList);
  }

  @SubscribeEvent
  public void tick(TickEvent.ServerTickEvent event) {
    legendarySpawner.tick();
  }
}
