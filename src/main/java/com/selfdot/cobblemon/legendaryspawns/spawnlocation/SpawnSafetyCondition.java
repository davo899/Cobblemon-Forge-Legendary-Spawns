package com.selfdot.cobblemon.legendaryspawns.spawnlocation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface SpawnSafetyCondition {

  boolean isSafe(Level level, BlockPos pos);

}
