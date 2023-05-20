package com.selfdot.cobblemon.legendaryspawns.spawnlocation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class AboveYLevel implements SpawnSafetyCondition {

  private final int yLevel;

  public AboveYLevel(int yLevel) {
    this.yLevel = yLevel;
  }

  @Override
  public boolean isSafe(Level level, BlockPos pos) {
    return pos.getY() > yLevel;
  }

}
