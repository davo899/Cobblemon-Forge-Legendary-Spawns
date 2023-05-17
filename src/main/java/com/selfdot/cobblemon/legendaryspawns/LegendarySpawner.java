package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class LegendarySpawner {
  private static final int SPAWN_INTERVAL_SECONDS = 60 * 60;
  public static final int SPAWN_INTERVAL_TICKS = 40 * SPAWN_INTERVAL_SECONDS;
  private static final int MINIMUM_SPAWN_DISTANCE = 32;
  private static final int MAXIMUM_SPAWN_DISTANCE = 128;
  private static final int MAXIMUM_SPAWN_ATTEMPTS = 5;
  private static final int MINIMUM_REQUIRED_PLAYERS = 1;

  private static final int SHINY_ODDS = 4096;

  private int spawnCountdown = SPAWN_INTERVAL_TICKS;

  private final MinecraftServer server;
  private final List<LegendarySpawn> legendarySpawnList;
  private final LightingStriker lightingStriker = new LightingStriker();

  public LegendarySpawner(MinecraftServer server, List<LegendarySpawn> legendarySpawnList) {
    this.server = server;
    this.legendarySpawnList = legendarySpawnList;
  }

  public void tick() {
    if (spawnCountdown > 0) spawnCountdown--;
    else {
      spawnLegendary();
      spawnCountdown = SPAWN_INTERVAL_TICKS;
    }
    lightingStriker.tick();
  }

  private void spawnLegendary() {
    List<ServerPlayer> players = server.getPlayerList().getPlayers();
    if (players.size() < MINIMUM_REQUIRED_PLAYERS) return;

    Optional<LegendarySpawn> chosenLegendaryOpt = legendarySpawnList.stream()
        .skip((int) (legendarySpawnList.size() * Math.random()))
        .findFirst();

    LegendarySpawn chosenLegendary;
    if (chosenLegendaryOpt.isPresent())
      chosenLegendary = chosenLegendaryOpt.get();
    else return;

    int attemptedSpawns = 0;
    Level spawnLevel = null;
    Vec3 spawnPos = null;

    while (spawnPos == null) {
      if (++attemptedSpawns > MAXIMUM_SPAWN_ATTEMPTS) {
        LogUtils.getLogger().info("Skipping Legendary spawn: Could not find safe spawn location after " +
            MAXIMUM_SPAWN_ATTEMPTS + " attempts");
        return;
      }
      // Choose random player
      Optional<ServerPlayer> chosenPlayerOpt = players.stream()
          .filter(player -> player.level.dimension() == Level.OVERWORLD)
          .skip((int) (players.size() * Math.random()))
          .findFirst();

      if (chosenPlayerOpt.isPresent()) {
        ServerPlayer chosenPlayer = chosenPlayerOpt.get();
        spawnPos = randomNearbySurfacePoint(chosenPlayer.level, chosenPlayer.getPosition(0f));
        spawnLevel = chosenPlayer.level;
      }
    }

    Pokemon legendary = new Pokemon();
    legendary.setSpecies(chosenLegendary.species);
    legendary.setLevel(chosenLegendary.level);
    if (Math.random() < (1d / SHINY_ODDS)) legendary.setShiny(true);

    PokemonEntity pokemonEntity = new PokemonEntity(
        spawnLevel,
        legendary,
        CobblemonEntities.POKEMON.get()
    );
    pokemonEntity.setDespawner(LegendaryDespawner.getInstance());
    pokemonEntity.setPos(spawnPos);
    spawnLevel.addFreshEntity(pokemonEntity);

    lightingStriker.setTracked(pokemonEntity);

    server.getPlayerList().broadcastSystemMessage(
        Component.literal("A Legendary ")
            .append(pokemonEntity.getPokemon().getSpecies().getTranslatedName().withStyle(ChatFormatting.GOLD))
            .append(" has spawned!"),
        false
    );
  }

  @Nullable
  private Vec3 randomNearbySurfacePoint(Level level, Vec3 centre) {
    double dist = MINIMUM_SPAWN_DISTANCE + ((MAXIMUM_SPAWN_DISTANCE - MINIMUM_SPAWN_DISTANCE) * Math.random());
    double theta = 2 * Math.PI * Math.random();
    double x = centre.x + (dist * Math.cos(theta));
    double z = centre.z + (dist * Math.sin(theta));
    BlockPos pos = new BlockPos(x, centre.y, z);
    while (level.getBlockState(pos).getMaterial().equals(Material.AIR) && level.isInWorldBounds(pos)) pos = pos.below();
    while (!level.getBlockState(pos).getMaterial().equals(Material.AIR) && level.isInWorldBounds(pos)) pos = pos.above();
    return level.isInWorldBounds(pos) && !isUnsafeSpawn(level, pos) ? new Vec3(x, pos.getY(), z) : null;
  }

  private boolean isUnsafeSpawn(Level level, BlockPos pos) {
    while (level.getBlockState(pos).getMaterial().equals(Material.AIR) && level.isInWorldBounds(pos)) pos = pos.below();
    return level.getBlockState(pos).getFluidState().is(FluidTags.LAVA);
  }
}
