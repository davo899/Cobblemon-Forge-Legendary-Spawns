package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.api.entity.Despawner;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import org.jetbrains.annotations.NotNull;

public class LegendaryDespawner implements Despawner<PokemonEntity> {

  private static final LegendaryDespawner instance = new LegendaryDespawner();
  public static LegendaryDespawner getInstance() { return instance; }
  private LegendaryDespawner() { }

  private int minimumDespawnDistance;
  private int spawnIntervalTicks;

  public void setMinimumDespawnDistance(int minimumDespawnDistance) {
    this.minimumDespawnDistance = minimumDespawnDistance;
  }

  public void setSpawnIntervalTicks(int spawnIntervalTicks) {
    this.spawnIntervalTicks = spawnIntervalTicks;
  }

  @Override
  public void beginTracking(@NotNull PokemonEntity pokemonEntity) { }

  @Override
  public boolean shouldDespawn(@NotNull PokemonEntity pokemonEntity) {
    if (pokemonEntity.getTicksLived() < spawnIntervalTicks) return false;

    return !pokemonEntity.level.hasNearbyAlivePlayer(
        pokemonEntity.getX(),
        pokemonEntity.getY(),
        pokemonEntity.getZ(),
        minimumDespawnDistance
    );
  }
}
