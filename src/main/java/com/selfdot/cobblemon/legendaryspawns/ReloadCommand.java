package com.selfdot.cobblemon.legendaryspawns;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ReloadCommand {

  public ReloadCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("randomlegendaryspawn")
        .then(Commands.literal("reload")
            .executes((command) -> reload(command.getSource())))
    );
  }

  private int reload(CommandSourceStack sourceStack) {
    if (LegendarySpawner.getInstance().loadConfig()) {
      sourceStack.sendSuccess(Component.literal("Reloaded Legendary Spawns config"), false);
      return 1;
    } else {
      sourceStack.sendFailure(Component.literal("Failed to load config"));
      return -1;
    }
  }

}
