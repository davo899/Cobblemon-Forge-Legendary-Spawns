package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class LegendaryCaptureListener extends Thread {

  private final PokemonEntity tracked;
  private final String captureAnnouncement;
  private final MinecraftServer server;

  public LegendaryCaptureListener(PokemonEntity tracked, String captureAnnouncement, MinecraftServer server) {
    this.tracked = tracked;
    this.captureAnnouncement = captureAnnouncement;
    this.server = server;
  }

  @Override
  public void run() {
    while (true) {
      PokemonCapturedEvent pokemonCapturedEvent = CobblemonEvents.POKEMON_CAPTURED.await();
      if (pokemonCapturedEvent.component1().getUuid() == tracked.getPokemon().getUuid()) {
        server.getPlayerList().broadcastSystemMessage(
            Component.literal(ChatColourUtils.format(captureAnnouncement)
                .replaceAll(ConfigKey.LEGENDARY_OR_ULTRA_BEAST_TOKEN, tracked.getPokemon().getSpecies().getTranslatedName().getString())
                .replaceAll(ConfigKey.PLAYER_TOKEN, pokemonCapturedEvent.component2().getDisplayName().getString())
            ),
            false
        );
        break;
      } else if (tracked.isRemoved()) break;
    }
  }

}
