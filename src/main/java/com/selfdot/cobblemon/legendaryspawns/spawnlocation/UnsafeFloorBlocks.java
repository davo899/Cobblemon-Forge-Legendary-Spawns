package com.selfdot.cobblemon.legendaryspawns.spawnlocation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Material;

import java.util.List;

public class UnsafeFloorBlocks implements SpawnSafetyCondition {

  private final List<Material> blocks;

  public UnsafeFloorBlocks(List<Material> blocks) {
    this.blocks = blocks;
  }

  @Override
  public boolean isSafe(Level level, BlockPos pos) {
    while (level.getBlockState(pos).getMaterial().equals(Material.AIR) && level.isInWorldBounds(pos)) pos = pos.below();
    return !blocks.contains(level.getBlockState(pos).getMaterial());
  }
}
