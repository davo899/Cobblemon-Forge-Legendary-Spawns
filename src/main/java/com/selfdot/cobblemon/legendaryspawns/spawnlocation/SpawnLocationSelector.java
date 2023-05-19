package com.selfdot.cobblemon.legendaryspawns.spawnlocation;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public interface SpawnLocationSelector {

  @Nullable
  Vec3 getSpawnLocation(Level level, Vec3 centre);

}
