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
import java.util.Map;
import java.util.SortedMap;

import static net.pldyn.bluemapzones.ConfigHandler.DEFAULT_MARKER_SET_LEVEL;
import static net.pldyn.bluemapzones.ConfigHandler.addMarkerSet;
import static net.pldyn.bluemapzones.ConfigHandler.getMarkerSetLevels;
import static net.pldyn.bluemapzones.ConfigHandler.getMarkerSetsByLevel;
import static net.pldyn.bluemapzones.ConfigHandler.removeMarkerSet;
import static net.pldyn.bluemapzones.ConfigHandler.setMarkerSetLevel;
import static net.pldyn.bluemapzones.MessageHandler.send;

/**
 * Manages which BlueMap marker sets zones are built from, and what level each sits at,
 * so the config never has to be edited by hand. Available ids are read live from the
 * BlueMap API.
 *
 * <p>Level 1 is the broadest tier and higher numbers nest inside it. Sets sharing a
 * level form one merged zone space.</p>
 */
public class markerSetCommand implements TabExecutor {

  private static final List<String> SUBCOMMANDS = List.of( "add", "remove", "level", "list" );

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
      case "level" -> doLevel( sender, args );
      default -> {
        send( sender, "Unknown subcommand '" + args[0] + "'.", NamedTextColor.RED );
        sendUsage( sender );
      }
    }

    return true;
  }

  /**
   * @method doList - Show configured sets grouped by level, and what else is available.
   * @param sender Whoever ran the command.
   */
  private void doList(CommandSender sender) {
    SortedMap<Integer, List<String>> byLevel = getMarkerSetsByLevel();

    if (byLevel.isEmpty()) {
      send( sender, "No marker sets are configured.", NamedTextColor.YELLOW );
    }
    else {
      send( sender, "Configured marker sets (level 1 is broadest):", NamedTextColor.AQUA );

      for (Map.Entry<Integer, List<String>> entry : byLevel.entrySet()) {
        send( sender, "  Level " + entry.getKey() + ": "
            + String.join( ", ", entry.getValue() ), NamedTextColor.AQUA );
      }
    }

    List<String> available = availableMarkerSets( sender );
    if (available == null) return;

    available.removeAll( getMarkerSetLevels().keySet() );

    send( sender, "Available to add: "
        + (available.isEmpty() ? "(none)" : String.join( ", ", available )),
        NamedTextColor.AQUA );
  }

  /**
   * @method doAdd - Add a marker set at a level, then rebuild zones.
   * @param sender Whoever ran the command.
   * @param args The raw command arguments.
   */
  private void doAdd(CommandSender sender, String[] args) {
    if (args.length < 2) {
      send( sender, "Usage: /bmz-markerset add <marker-set> [level]", NamedTextColor.RED );
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

    Integer level = args.length >= 3 ? parseLevel( sender, args[2] ) : DEFAULT_MARKER_SET_LEVEL;
    if (level == null) return;

    if (!addMarkerSet( markerSetId, level )) {
      send( sender, "Marker set '" + markerSetId + "' is already configured.",
          NamedTextColor.YELLOW );
      return;
    }

    send( sender, "Added marker set '" + markerSetId + "' at level " + level + ".",
        NamedTextColor.GREEN );
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
      send( sender, "Marker set '" + markerSetId + "' is not configured. Configured: "
          + describeConfigured(), NamedTextColor.RED );
      return;
    }

    send( sender, "Removed marker set '" + markerSetId + "'.", NamedTextColor.GREEN );
    regenerate( sender );
  }

  /**
   * @method doLevel - Move an already configured marker set to a different level.
   * @param sender Whoever ran the command.
   * @param args The raw command arguments.
   */
  private void doLevel(CommandSender sender, String[] args) {
    if (args.length < 3) {
      send( sender, "Usage: /bmz-markerset level <marker-set> <level>", NamedTextColor.RED );
      return;
    }

    String markerSetId = args[1];
    Integer level = parseLevel( sender, args[2] );
    if (level == null) return;

    if (!setMarkerSetLevel( markerSetId, level )) {
      send( sender, "Marker set '" + markerSetId + "' is not configured. Configured: "
          + describeConfigured(), NamedTextColor.RED );
      return;
    }

    send( sender, "Moved marker set '" + markerSetId + "' to level " + level + ".",
        NamedTextColor.GREEN );
    regenerate( sender );
  }

  /**
   * @method parseLevel - Read and validate a level argument.
   * @param sender Whoever ran the command, so the failure can be explained.
   * @param raw The raw argument.
   * @return The level, or null if it was not a positive whole number.
   */
  private Integer parseLevel(CommandSender sender, String raw) {
    int level;

    try {
      level = Integer.parseInt( raw );
    }
    catch (NumberFormatException levelErr) {
      send( sender, "Level must be a whole number, got '" + raw + "'.", NamedTextColor.RED );
      return null;
    }

    if (level < 1) {
      send( sender, "Level must be 1 or higher. Level 1 is the broadest tier.",
          NamedTextColor.RED );
      return null;
    }

    return level;
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

  private String describeConfigured() {
    List<String> configured = new ArrayList<>( getMarkerSetLevels().keySet() );
    return configured.isEmpty() ? "(none)" : String.join( ", ", configured );
  }

  private void sendUsage(CommandSender sender) {
    send( sender, "Usage: /bmz-markerset <add|remove|level|list> [marker-set] [level]",
        NamedTextColor.YELLOW );
    send( sender, "Level 1 is broadest; higher levels nest inside it. Sets sharing a "
        + "level merge into one zone space.", NamedTextColor.GRAY );
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
    if (args.length == 1) {
      return StringUtil.copyPartialMatches( args[0], SUBCOMMANDS, new ArrayList<>() );
    }

    if (args.length == 2) {
      // "add" offers what BlueMap has that is not configured yet; the others
      // operate on what is already configured. "list" takes no argument.
      switch (args[0].toLowerCase()) {
        case "add" -> {
          BlueMapMap map = BlueMap_Zones.getInstance().getConfiguredMap();
          if (map == null) return new ArrayList<>();

          List<String> candidates = new ArrayList<>( map.getMarkerSets().keySet() );
          candidates.removeAll( getMarkerSetLevels().keySet() );

          return StringUtil.copyPartialMatches( args[1], candidates, new ArrayList<>() );
        }
        case "remove", "level" -> {
          return StringUtil.copyPartialMatches( args[1],
              new ArrayList<>( getMarkerSetLevels().keySet() ), new ArrayList<>() );
        }
      }
    }

    if (args.length == 3 && (args[0].equalsIgnoreCase( "add" ) || args[0].equalsIgnoreCase( "level" ))) {
      return StringUtil.copyPartialMatches( args[2], suggestLevels(), new ArrayList<>() );
    }

    return new ArrayList<>();
  }

  /**
   * @method suggestLevels - Levels already in use, plus the next one down.
   * @return {List<String>}
   */
  private List<String> suggestLevels() {
    SortedMap<Integer, List<String>> byLevel = getMarkerSetsByLevel();
    List<String> levels = new ArrayList<>();

    for (Integer level : byLevel.keySet()) {
      levels.add( String.valueOf( level ) );
    }

    int next = byLevel.isEmpty() ? DEFAULT_MARKER_SET_LEVEL : byLevel.lastKey() + 1;
    levels.add( String.valueOf( next ) );

    return levels;
  }
}
