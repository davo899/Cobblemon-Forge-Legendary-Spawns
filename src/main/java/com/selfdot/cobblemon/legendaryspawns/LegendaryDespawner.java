package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.api.entity.Despawner;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import org.jetbrains.annotations.NotNull;

public class LegendaryDespawner implements Despawner<PokemonEntity> {

  private static final LegendaryDespawner instance = new LegendaryDespawner();

  private LegendaryDespawner() { }

  public static LegendaryDespawner getInstance() {
    return instance;
  }

  private static final int MINIMUM_DESPAWN_DISTANCE = 32;

  @Override
  public void beginTracking(@NotNull PokemonEntity pokemonEntity) { }

  @Override
  public boolean shouldDespawn(@NotNull PokemonEntity pokemonEntity) {
    if (pokemonEntity.getTicksLived() < LegendarySpawner.SPAWN_INTERVAL_TICKS) return false;

    return !pokemonEntity.level.hasNearbyAlivePlayer(
        pokemonEntity.getX(),
        pokemonEntity.getY(),
        pokemonEntity.getZ(),
        MINIMUM_DESPAWN_DISTANCE
    );
  }
}
