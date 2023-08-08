package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.command.ConfigCommand;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(LegendarySpawnsMod.MODID)
public class LegendarySpawnsMod {

  // Define mod id in a common place for everything to reference
  public static final String MODID = "legendaryspawns";
  // Directly reference a slf4j logger
  public static final Logger LOGGER = LogUtils.getLogger();

  public LegendarySpawnsMod() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    // Register the commonSetup method for modloading
    modEventBus.addListener(this::commonSetup);

    // Register ourselves for server and other game events we are interested in
    MinecraftForge.EVENT_BUS.register(this);
  }

  private void commonSetup(final FMLCommonSetupEvent event) { }

  @SubscribeEvent
  public void onServerStart(ServerStartingEvent event) {
    try {
      LegendarySpawner legendarySpawner = LegendarySpawner.getInstance();
      legendarySpawner.setServer(event.getServer());
      if (legendarySpawner.loadConfig()) LOGGER.info("Loaded config successfully");
      else LOGGER.error("Legendary Spawns failed to load config");

    } catch (Throwable t) {
      LOGGER.warn("An exception occurred in LegendarySpawns.onServerStart()");
      t.printStackTrace();
    }
  }

  @SubscribeEvent
  public void tick(TickEvent.ServerTickEvent event) {
    try {
      LegendarySpawner.getInstance().tick();

    } catch (Throwable t) {
      LOGGER.warn("An exception occurred in LegendarySpawns.tick()");
      t.printStackTrace();
    }
  }

  @SubscribeEvent
  public void onCommandsRegister(RegisterCommandsEvent event) {
    new ReloadCommand(event.getDispatcher());
    ConfigCommand.register(event.getDispatcher());
  }

}
