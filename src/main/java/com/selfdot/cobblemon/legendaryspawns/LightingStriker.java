package com.selfdot.cobblemon.legendaryspawns;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public class LightingStriker {

  private final int strikeInterval;
  private int strikeCountdown = 0;
  private Entity tracked = null;

  public LightingStriker(int strikeInterval) {
    this.strikeInterval = strikeInterval;
  }

  public void setTracked(Entity tracked) {
    this.tracked = tracked;
  }

  public void tick() {
    if (strikeCountdown > 0) strikeCountdown--;
    else if (tracked != null) {
      LightningBolt lightningEntity = new LightningBolt(EntityType.LIGHTNING_BOLT, tracked.level);
      Vec3 strikePos = tracked.position().add(0, 5, 0);
      lightningEntity.setPos(strikePos);
      tracked.level.addFreshEntity(lightningEntity);
      strikeCountdown = strikeInterval;
    }
  }

}
