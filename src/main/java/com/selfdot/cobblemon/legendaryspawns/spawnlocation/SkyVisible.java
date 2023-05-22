package com.selfdot.cobblemon.legendaryspawns.spawnlocation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class SkyVisible implements SpawnSafetyCondition {

  private final boolean skyVisible;

  public SkyVisible(boolean skyVisible) {
    this.skyVisible = skyVisible;
  }

  @Override
  public boolean isSafe(Level level, BlockPos pos) {
    return level.canSeeSky(pos) == skyVisible;
  }

}
