package com.selfdot.cobblemon.legendaryspawns;

import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ChatColourUtils {

  public static String format(String s) {
    return s.replace("&0", "" + ChatFormatting.BLACK).replace("&1", "" + ChatFormatting.DARK_BLUE)
        .replace("&2", "" + ChatFormatting.DARK_GREEN).replace("&3", "" + ChatFormatting.DARK_AQUA)
        .replace("&4", "" + ChatFormatting.DARK_RED).replace("&5", "" + ChatFormatting.DARK_PURPLE)
        .replace("&6", "" + ChatFormatting.GOLD).replace("&7", "" + ChatFormatting.GRAY)
        .replace("&8", "" + ChatFormatting.DARK_GRAY).replace("&9", "" + ChatFormatting.BLUE)
        .replace("&a", "" + ChatFormatting.GREEN).replace("&b", "" + ChatFormatting.AQUA)
        .replace("&c", "" + ChatFormatting.RED).replace("&d", "" + ChatFormatting.LIGHT_PURPLE)
        .replace("&e", "" + ChatFormatting.YELLOW).replace("&f", "" + ChatFormatting.WHITE)
        .replace("&k", "" + ChatFormatting.OBFUSCATED).replace("&l", "" + ChatFormatting.BOLD)
        .replace("&m", "" + ChatFormatting.STRIKETHROUGH).replace("&n", "" + ChatFormatting.UNDERLINE)
        .replace("&o", "" + ChatFormatting.ITALIC).replace("&r", "" + ChatFormatting.RESET)
        .replace("&A", "" + ChatFormatting.GREEN).replace("&B", "" + ChatFormatting.AQUA)
        .replace("&C", "" + ChatFormatting.RED).replace("&D", "" + ChatFormatting.LIGHT_PURPLE)
        .replace("&E", "" + ChatFormatting.YELLOW).replace("&F", "" + ChatFormatting.WHITE)
        .replace("&K", "" + ChatFormatting.OBFUSCATED).replace("&L", "" + ChatFormatting.BOLD)
        .replace("&M", "" + ChatFormatting.STRIKETHROUGH).replace("&N", "" + ChatFormatting.UNDERLINE)
        .replace("&O", "" + ChatFormatting.ITALIC).replace("&R", "" + ChatFormatting.RESET);
  }

  public static Component formattedAnnouncement(String announcement, Pokemon pokemon, ServerPlayer player) {
    return Component.literal(ChatColourUtils.format(announcement)
        .replaceAll(ConfigKey.POKEMON_TOKEN, pokemon.getSpecies().getTranslatedName().getString())
        .replaceAll(ConfigKey.PLAYER_TOKEN, player.getDisplayName().getString())
    );
  }

}
