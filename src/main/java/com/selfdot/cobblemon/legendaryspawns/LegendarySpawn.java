package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.pokemon.Species;

public class LegendarySpawn {
  public final Species species;
  public final int level;

  public LegendarySpawn(Species species, int level) {
    this.species = species;
    this.level = level;
  }
}
