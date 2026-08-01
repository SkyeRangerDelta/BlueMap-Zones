package net.pldyn.bluemapzones.commands;

import net.kyori.adventure.text.format.NamedTextColor;
import net.pldyn.bluemapzones.BlueMap_Zones;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.pldyn.bluemapzones.MessageHandler.send;

public class generateCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        BlueMap_Zones plugin = BlueMap_Zones.getInstance();

        // This command takes no arguments; it always rebuilds from the configured
        // marker sets. Say so rather than silently ignoring what was typed.
        if ( args.length > 0 ) {
            send(sender, "/bmz-generate takes no arguments - it rebuilds from the "
                + "configured marker set. Use /bmz-markerset to change it.",
                NamedTextColor.RED);
            return true;
        }

        if ( plugin.isGenerating() ) {
            send(sender, "Generation already in progress!", NamedTextColor.RED);
            return true;
        }

        send(sender, "Generating zones...", NamedTextColor.GREEN);

        plugin.generateZones();

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        // Takes no arguments. Returning empty rather than null stops Bukkit
        // falling back to suggesting online player names.
        return new ArrayList<>();
    }
}
