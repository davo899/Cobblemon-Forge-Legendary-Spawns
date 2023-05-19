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

  private static final int TICKS_PER_SECOND = 40;

  private final MinecraftServer server;
  private final List<LegendarySpawn> legendarySpawnList;
  private final int spawnIntervalTicks;
  private final int minimumSpawnDistance;
  private final int maximumSpawnDistance;
  private final int maximumSpawnAttempts;
  private final int minimumRequiredPlayers;
  private final int shinyOdds;

  private int spawnCountdown;
  private final LightingStriker lightingStriker;

  public LegendarySpawner(
      MinecraftServer server,
      List<LegendarySpawn> legendarySpawnList,
      int spawnIntervalSeconds,
      int minimumSpawnDistance,
      int maximumSpawnDistance,
      int maximumSpawnAttempts,
      int minimumRequiredPlayers,
      int shinyOdds,
      int lightningStrikesPerSpawn
  ) {
    this.server = server;
    this.legendarySpawnList = legendarySpawnList;
    this.spawnIntervalTicks = spawnIntervalSeconds * TICKS_PER_SECOND;
    this.minimumSpawnDistance = minimumSpawnDistance;
    this.maximumSpawnDistance = maximumSpawnDistance;
    this.maximumSpawnAttempts = maximumSpawnAttempts;
    this.minimumRequiredPlayers = minimumRequiredPlayers;
    this.shinyOdds = shinyOdds;
    this.lightingStriker = new LightingStriker(spawnIntervalTicks / lightningStrikesPerSpawn);

    spawnCountdown = spawnIntervalTicks;
  }

  public void tick() {
    if (spawnCountdown > 0) spawnCountdown--;
    else {
      spawnLegendary();
      spawnCountdown = spawnIntervalTicks;
    }
    lightingStriker.tick();
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
    Level spawnLevel = null;
    Vec3 spawnPos = null;

    while (spawnPos == null) {
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
        ServerPlayer chosenPlayer = chosenPlayerOpt.get();
        spawnPos = randomNearbySurfacePoint(chosenPlayer.level, chosenPlayer.getPosition(0f));
        spawnLevel = chosenPlayer.level;
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
    pokemonEntity.setDespawner(new LegendaryDespawner(minimumSpawnDistance, spawnIntervalTicks));
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
    double dist = minimumSpawnDistance + ((maximumSpawnDistance - minimumSpawnDistance) * Math.random());
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
