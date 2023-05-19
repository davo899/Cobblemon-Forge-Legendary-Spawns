package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.api.entity.Despawner;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import org.jetbrains.annotations.NotNull;

public class LegendaryDespawner implements Despawner<PokemonEntity> {

  private final int minimumDespawnDistance;
  private final int spawnIntervalDistance;

  public LegendaryDespawner(int minimumDespawnDistance, int spawnIntervalDistance) {
    this.minimumDespawnDistance = minimumDespawnDistance;
    this.spawnIntervalDistance = spawnIntervalDistance;
  }

  @Override
  public void beginTracking(@NotNull PokemonEntity pokemonEntity) { }

  @Override
  public boolean shouldDespawn(@NotNull PokemonEntity pokemonEntity) {
    if (pokemonEntity.getTicksLived() < spawnIntervalDistance) return false;

    return !pokemonEntity.level.hasNearbyAlivePlayer(
        pokemonEntity.getX(),
        pokemonEntity.getY(),
        pokemonEntity.getZ(),
        minimumDespawnDistance
    );
  }
}
