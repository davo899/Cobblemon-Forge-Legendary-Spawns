package com.selfdot.cobblemon.legendaryspawns.spawnlocation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class RandomNearbySurfacePoint implements SpawnLocationSelector {

  private static final int SEA_LEVEL = 64;

  private final int minimumSpawnDistance;
  private final int maximumSpawnDistance;

  public RandomNearbySurfacePoint(int minimumSpawnDistance, int maximumSpawnDistance) {
    this.minimumSpawnDistance = minimumSpawnDistance;
    this.maximumSpawnDistance = maximumSpawnDistance;
  }

  @Nullable
  @Override
  public Vec3 getSpawnLocation(Level level, Vec3 centre) {
    double dist = minimumSpawnDistance + ((maximumSpawnDistance - minimumSpawnDistance) * Math.random());
    double theta = 2 * Math.PI * Math.random();
    double x = centre.x + (dist * Math.cos(theta));
    double z = centre.z + (dist * Math.sin(theta));
    BlockPos pos = new BlockPos(x, SEA_LEVEL, z);
    while (level.getBlockState(pos).getMaterial().equals(Material.AIR) && level.isInWorldBounds(pos)) pos = pos.below();
    while (!level.getBlockState(pos).getMaterial().equals(Material.AIR) && level.isInWorldBounds(pos)) pos = pos.above();
    return level.isInWorldBounds(pos) ? new Vec3(x, pos.getY(), z) : null;
  }
}
