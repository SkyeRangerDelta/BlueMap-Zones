package net.pldyn.bluemapzones.commands;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.pldyn.bluemapzones.ConfigHandler.reloadPluginConfigFile;
import static net.pldyn.bluemapzones.MessageHandler.send;

public class reloadConfCommand implements TabExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    reloadPluginConfigFile();
    send( sender, "BlueMap-Zones configuration reloaded.", NamedTextColor.GREEN );

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
    // Takes no arguments. Returning empty rather than null stops Bukkit
    // falling back to suggesting online player names.
    return new ArrayList<>();
  }
}
