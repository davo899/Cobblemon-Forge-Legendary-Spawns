package com.selfdot.cobblemon.legendaryspawns;

import net.minecraft.ChatFormatting;

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

}
