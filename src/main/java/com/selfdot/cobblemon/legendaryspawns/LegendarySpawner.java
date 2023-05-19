package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
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
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LegendarySpawner {

  private static final int TICKS_PER_SECOND = 40;

  private final MinecraftServer server;
  private final List<LegendarySpawn> legendarySpawnList;
  private final int spawnIntervalTicks;
  private final int maximumSpawnAttempts;
  private final int minimumRequiredPlayers;
  private final int shinyOdds;

  private int spawnCountdown;
  private final LightingStriker lightingStriker;
  private final SpawnLocationSelector spawnLocationSelector;
  private final List<SpawnSafetyCondition> spawnSafetyConditions;
  private final LegendaryDespawner legendaryDespawner;

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
    this.maximumSpawnAttempts = maximumSpawnAttempts;
    this.minimumRequiredPlayers = minimumRequiredPlayers;
    this.shinyOdds = shinyOdds;
    this.spawnCountdown = spawnIntervalTicks;
    this.lightingStriker = new LightingStriker(spawnIntervalTicks / lightningStrikesPerSpawn);
    this.spawnLocationSelector = new RandomNearbyPoint(minimumSpawnDistance, maximumSpawnDistance);
    this.spawnSafetyConditions = List.of(new UnsafeFloorBlocks(List.of(Material.FIRE, Material.LAVA, Material.CACTUS)));
    this.legendaryDespawner = new LegendaryDespawner(minimumSpawnDistance, spawnIntervalTicks);
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
    Level spawnLevel;
    Vec3 spawnPos;

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
        ServerPlayer chosenPlayer = chosenPlayerOpt.get();
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
    pokemonEntity.setDespawner(legendaryDespawner);
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
}
