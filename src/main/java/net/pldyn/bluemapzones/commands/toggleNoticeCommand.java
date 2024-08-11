package net.pldyn.bluemapzones.commands;

import net.pldyn.bluemapzones.BlueMap_Zones;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

import static net.pldyn.bluemapzones.ConfigHandler.*;

public class toggleNoticeCommand implements CommandExecutor {

  private final BlueMap_Zones plugin = BlueMap_Zones.getInstance();

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    if (sender instanceof Player player) {
      UUID pid =  player.getUniqueId();

      List<UUID> exclusionsList = getNoticeExclusions();

      if (exclusionsList.contains( pid )) {
        removeNoticeExclusion( pid );
        player.sendMessage("You will now receive zone notices.");
      } else {
        addNoticeExclusion( pid );
        player.sendMessage("You will no longer receive zone notices.");
      }

      return true;
    }

    return false;
  }
}
