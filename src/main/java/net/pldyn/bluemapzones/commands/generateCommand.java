package net.pldyn.bluemapzones.commands;

import net.kyori.adventure.text.format.NamedTextColor;
import net.pldyn.bluemapzones.BlueMap_Zones;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.pldyn.bluemapzones.MessageHandler.send;

public class generateCommand implements CommandExecutor {

    private final BlueMap_Zones plugin = BlueMap_Zones.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        send(sender, "Generating zones...", NamedTextColor.GREEN);

        plugin.generateZones();

        return true;
    }
}
