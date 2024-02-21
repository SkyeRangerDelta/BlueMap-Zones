package net.pldyn.bluemapzones.commands;

import net.pldyn.bluemapzones.BlueMap_Zones;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class generateCommand implements CommandExecutor {

    private final BlueMap_Zones plugin = BlueMap_Zones.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            player.sendMessage("Starting zone generation...");

            plugin.generateZones(plugin.getBlueMapAPI());

            player.sendMessage("Generating zones...");
            return true;
        }

        return false;
    }
}
