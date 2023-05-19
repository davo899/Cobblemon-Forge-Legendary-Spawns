package com.selfdot.cobblemon.legendaryspawns;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public class LightingStriker {

  private static final LightingStriker instance = new LightingStriker();
  public static LightingStriker getInstance() { return instance; }
  private LightingStriker() { }

  private int strikeInterval;
  private int strikeCountdown = 0;
  private Entity tracked = null;

  public void setStrikeInterval(int strikeInterval) {
    this.strikeCountdown = Math.min(strikeInterval, strikeCountdown);
    this.strikeInterval = strikeInterval;
  }

  public void setTracked(Entity tracked) {
    this.tracked = tracked;
  }

  public void tick() {
    if (strikeCountdown > 0) strikeCountdown--;
    else if (!(tracked == null || tracked.isRemoved())) {
      LightningBolt lightningEntity = new LightningBolt(EntityType.LIGHTNING_BOLT, tracked.level);
      Vec3 strikePos = tracked.position().add(0, 5, 0);
      lightningEntity.setPos(strikePos);
      tracked.level.addFreshEntity(lightningEntity);
      strikeCountdown = strikeInterval;
    }
  }

}
