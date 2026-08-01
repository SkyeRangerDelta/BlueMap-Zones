package net.pldyn.bluemapzones.commands;

import de.bluecolored.bluemap.api.BlueMapMap;
import net.kyori.adventure.text.format.NamedTextColor;
import net.pldyn.bluemapzones.BlueMap_Zones;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.pldyn.bluemapzones.ConfigHandler.addMarkerSet;
import static net.pldyn.bluemapzones.ConfigHandler.getMarkerSets;
import static net.pldyn.bluemapzones.ConfigHandler.removeMarkerSet;
import static net.pldyn.bluemapzones.MessageHandler.send;

/**
 * Manages which BlueMap marker sets zones are built from, so the config never
 * has to be edited by hand. Available ids are read live from the BlueMap API.
 */
public class markerSetCommand implements TabExecutor {

  private static final List<String> SUBCOMMANDS = List.of( "add", "remove", "list" );

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    if (args.length == 0) {
      sendUsage( sender );
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "list" -> doList( sender );
      case "add" -> doAdd( sender, args );
      case "remove" -> doRemove( sender, args );
      default -> {
        send( sender, "Unknown subcommand '" + args[0] + "'.", NamedTextColor.RED );
        sendUsage( sender );
      }
    }

    return true;
  }

  /**
   * @method doList - Show which marker sets are configured and which are available.
   * @param sender Whoever ran the command.
   */
  private void doList(CommandSender sender) {
    List<String> configured = getMarkerSets();
    List<String> available = availableMarkerSets( sender );

    send( sender, "Configured marker sets: "
        + (configured.isEmpty() ? "(none)" : String.join( ", ", configured )),
        NamedTextColor.AQUA );

    if (available == null) return;

    send( sender, "Available on this map: "
        + (available.isEmpty() ? "(none)" : String.join( ", ", available )),
        NamedTextColor.AQUA );
  }

  /**
   * @method doAdd - Add a marker set to the config, then rebuild zones.
   * @param sender Whoever ran the command.
   * @param args The raw command arguments.
   */
  private void doAdd(CommandSender sender, String[] args) {
    if (args.length < 2) {
      send( sender, "Usage: /bmz-markerset add <marker-set>", NamedTextColor.RED );
      return;
    }

    String markerSetId = args[1];
    List<String> available = availableMarkerSets( sender );
    if (available == null) return;

    // Reject typos rather than writing an id that will never resolve.
    if (!available.contains( markerSetId )) {
      send( sender, "No marker set '" + markerSetId + "' on this map. Available: "
          + (available.isEmpty() ? "(none)" : String.join( ", ", available )),
          NamedTextColor.RED );
      return;
    }

    if (!addMarkerSet( markerSetId )) {
      send( sender, "Marker set '" + markerSetId + "' is already configured.",
          NamedTextColor.YELLOW );
      return;
    }

    send( sender, "Added marker set '" + markerSetId + "'.", NamedTextColor.GREEN );
    regenerate( sender );
  }

  /**
   * @method doRemove - Remove a marker set from the config, then rebuild zones.
   * @param sender Whoever ran the command.
   * @param args The raw command arguments.
   */
  private void doRemove(CommandSender sender, String[] args) {
    if (args.length < 2) {
      send( sender, "Usage: /bmz-markerset remove <marker-set>", NamedTextColor.RED );
      return;
    }

    String markerSetId = args[1];

    if (!removeMarkerSet( markerSetId )) {
      List<String> configured = getMarkerSets();
      send( sender, "Marker set '" + markerSetId + "' is not configured. Configured: "
          + (configured.isEmpty() ? "(none)" : String.join( ", ", configured )),
          NamedTextColor.RED );
      return;
    }

    send( sender, "Removed marker set '" + markerSetId + "'.", NamedTextColor.GREEN );
    regenerate( sender );
  }

  /**
   * @method regenerate - Kick off a zone rebuild so the change takes effect immediately.
   * @param sender Whoever ran the command.
   */
  private void regenerate(CommandSender sender) {
    BlueMap_Zones plugin = BlueMap_Zones.getInstance();

    if (plugin.isGenerating()) {
      send( sender, "Generation is already running; it will pick up the change.",
          NamedTextColor.YELLOW );
      return;
    }

    send( sender, "Regenerating zones...", NamedTextColor.GREEN );
    plugin.generateZones();
  }

  /**
   * @method availableMarkerSets - Marker set ids BlueMap currently has on the configured map.
   * @param sender Whoever ran the command, so the failure can be explained to them.
   * @return The available ids, or null if the configured map could not be resolved.
   */
  private List<String> availableMarkerSets(CommandSender sender) {
    BlueMapMap map = BlueMap_Zones.getInstance().getConfiguredMap();

    if (map == null) {
      send( sender, "Could not resolve the map set in Maps.name. Is BlueMap finished "
          + "loading?", NamedTextColor.RED );
      return null;
    }

    return new ArrayList<>( map.getMarkerSets().keySet() );
  }

  private void sendUsage(CommandSender sender) {
    send( sender, "Usage: /bmz-markerset <add|remove|list> [marker-set]",
        NamedTextColor.YELLOW );
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
    if (args.length == 1) {
      return StringUtil.copyPartialMatches( args[0], SUBCOMMANDS, new ArrayList<>() );
    }

    if (args.length == 2) {
      // "add" offers what BlueMap has that is not configured yet; "remove" offers
      // what is configured. "list" takes no argument.
      switch (args[0].toLowerCase()) {
        case "add" -> {
          BlueMapMap map = BlueMap_Zones.getInstance().getConfiguredMap();
          if (map == null) return new ArrayList<>();

          List<String> candidates = new ArrayList<>( map.getMarkerSets().keySet() );
          candidates.removeAll( getMarkerSets() );

          return StringUtil.copyPartialMatches( args[1], candidates, new ArrayList<>() );
        }
        case "remove" -> {
          return StringUtil.copyPartialMatches( args[1], getMarkerSets(), new ArrayList<>() );
        }
      }
    }

    return new ArrayList<>();
  }
}
