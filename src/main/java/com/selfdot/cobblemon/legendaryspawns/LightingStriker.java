package com.selfdot.cobblemon.legendaryspawns;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LightingStriker {

  private static final LightingStriker instance = new LightingStriker();
  public static LightingStriker getInstance() { return instance; }
  private LightingStriker() { }

  private int strikeInterval;
  private int strikeCountdown = 0;
  private List<Entity> tracked = new ArrayList<>();

  public void setStrikeInterval(int strikeInterval) {
    this.strikeCountdown = Math.min(strikeInterval, strikeCountdown);
    this.strikeInterval = strikeInterval;
  }

  public void addTracked(Entity entity) {
    tracked.add(entity);
  }

  public void tick() {
    if (strikeCountdown > 0) strikeCountdown--;
    else {
      tracked = tracked.stream().filter(e -> !e.isRemoved()).collect(Collectors.toList());
      for (Entity entity : tracked) {
        LightningBolt lightningEntity = new LightningBolt(EntityType.LIGHTNING_BOLT, entity.level);
        Vec3 strikePos = entity.position().add(0, 5, 0);
        lightningEntity.setPos(strikePos);
        entity.level.addFreshEntity(lightningEntity);
      }
      strikeCountdown = strikeInterval;
    }
  }

  private static void strikeTracked(Entity tracked) {

  }

}
