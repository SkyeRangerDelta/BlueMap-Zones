package net.pldyn.bluemapzones;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;

public class ConfigHandler {
  private static final Logger Log = Logger.getLogger("BM Zones");

  /** Section in BMZ-NoticeExclusions.yml mapping a player UUID to a NoticeType id. */
  private static final String NOTICES_PATH = "Notices";

  /**
   * What a legacy opt-out becomes on migration. Deliberately not OFF: a player who
   * silently receives nothing cannot tell the plugin is running, so they land on the
   * least intrusive type that still shows something.
   */
  private static final NoticeType MIGRATION_DEFAULT = NoticeType.CHAT;

  /** Section in BMZ-Config.yml mapping a BlueMap marker set id to its level. */
  private static final String MARKER_SETS_PATH = "Maps.marker-sets";

  /**
   * Level applied to a marker set the admin has not levelled. Level 1 is the broadest
   * tier, and higher numbers nest inside it. Everything defaulting here means an
   * un-levelled config is a single flat zone space, matching pre-levels behaviour.
   */
  public static final int DEFAULT_MARKER_SET_LEVEL = 1;

  private static File confFile;
  private static FileConfiguration pluginConfFile;

  private static File noticeExclusionsConf;
  private static FileConfiguration noticeExclusionsConfFile;

  private static final List<File> fileConfigurations = new ArrayList<>();

  private static BlueMap_Zones BMZ = BlueMap_Zones.getInstance();

  /**
   * @method confInit - Initialize the configuration files for the plugin.
   */
  public static void confInit() {
    // Create/Load configs
    filesInit();

    // Set defaults for plugin settings
    pluginConfFile.options().copyDefaults(true);
    savePluginConfFile();

    // Set defaults for notice UUIDs
    noticeExclusionsConfFile.options().copyDefaults(true);
    saveNoticeExclusionsConf();
  }

  /**
   * @method filesInit - Initialize the configuration files for the plugin.
   */
  private static void filesInit() {
    // Plugin configuration file
    confFile = new File(
      Bukkit.getPluginManager()
        .getPlugin("BlueMap-Zones")
        .getDataFolder(), "BMZ-Config.yml");

    if (!confFile.exists()) {
      try {
        confFile.createNewFile();
      }
      catch (IOException fileCreationErr) {
        Log.warning("Unable to create a config file on the server!\n" + fileCreationErr.getMessage());
      }
    }

    fileConfigurations.add(confFile);

    // Notice Exclusions Configuration File
    noticeExclusionsConf = new File(
      Bukkit.getPluginManager()
        .getPlugin("BlueMap-Zones")
        .getDataFolder(), "BMZ-NoticeExclusions.yml");

    if (!noticeExclusionsConf.exists()) {
      try {
        noticeExclusionsConf.createNewFile();
      }
      catch (IOException fileCreationErr) {
        Log.warning("Unable to create a notice exclusions file on the server!\n" + fileCreationErr.getMessage());
      }
    }

    fileConfigurations.add(noticeExclusionsConf);

    loadConfig();
  }

  /**
   * @method loadConfig - Load the configuration files for the plugin.
   */
  private static void loadConfig() {
    for (File f : fileConfigurations) {
      if (f.getName().contains("BMZ-Config")) {
        pluginConfFile = YamlConfiguration.loadConfiguration(f);
      }
      else if (f.getName().contains("BMZ-NoticeExclusions")) {
        noticeExclusionsConfFile = YamlConfiguration.loadConfiguration(f);
      }
    }
  }

  /**
   * @method createPluginDefaults - Create default values for the plugin configuration.
   */
  public static void createPluginDefaults() {
    List<String> headerComments = generateConfigComments();

    pluginConfFile.options().setHeader( headerComments );

    pluginConfFile.addDefault("Wilderness-Name", "Wilderness");
    pluginConfFile.addDefault("Maps.name", "world");
    pluginConfFile.addDefault("Maps.marker-sets", new ArrayList<String>());
    savePluginConfFile();

    normalizeMarkerSets();

    migrateNoticeExclusions();
    saveNoticeExclusionsConf();
  }

  /**
   * @method migrateNoticeExclusions - Convert legacy opt-out lists of UUIDs into the
   *     "Notices" section, which maps each UUID to a notice type. Handles both the old
   *     "Exclusions" key and a hand-written "Notices" that was written as a YAML list
   *     rather than a map. Migrated players get MIGRATION_DEFAULT rather than OFF, so a
   *     silent plugin never looks like a broken one. Safe to call repeatedly.
   */
  private static void migrateNoticeExclusions() {
    List<String> legacyUuids = new ArrayList<>( noticeExclusionsConfFile.getStringList("Exclusions") );
    String migratedFrom = "Exclusions";

    // "Notices:" written as a list is the shape the old Exclusions key used, and it
    // reads back as no preference at all. Treat it as a legacy opt-out list.
    if (noticeExclusionsConfFile.contains(NOTICES_PATH)
        && !noticeExclusionsConfFile.isConfigurationSection(NOTICES_PATH)) {
      List<String> strayUuids = noticeExclusionsConfFile.getStringList(NOTICES_PATH);

      Log.warning(NOTICES_PATH + " was a list, not a map of UUID to notice type. "
          + "Converting " + strayUuids.size() + " entry/entries.");

      legacyUuids.addAll(strayUuids);
      migratedFrom = migratedFrom + " and a malformed " + NOTICES_PATH;
      noticeExclusionsConfFile.set(NOTICES_PATH, null);
    }

    if (legacyUuids.isEmpty()) {
      noticeExclusionsConfFile.set("Exclusions", null);
      return;
    }

    for (String uuid : legacyUuids) {
      // Do not clobber a preference that already exists in the new format.
      if (noticeExclusionsConfFile.contains(NOTICES_PATH + "." + uuid)) continue;
      noticeExclusionsConfFile.set(NOTICES_PATH + "." + uuid, MIGRATION_DEFAULT.getId());
    }

    noticeExclusionsConfFile.set("Exclusions", null);

    Log.info("Migrated " + legacyUuids.size() + " notice preference(s) from " + migratedFrom
        + " to " + MIGRATION_DEFAULT.getId() + ".");
  }

  /**
   * @method normalizeMarkerSets - Accept Maps.marker-sets written either as a plain list
   *     of ids or as a map of id to level, and store it as the map form. A plain list
   *     means every set sits at DEFAULT_MARKER_SET_LEVEL, which is exactly the flat
   *     single-space behaviour the plugin had before levels existed.
   */
  private static void normalizeMarkerSets() {
    if (!pluginConfFile.contains(MARKER_SETS_PATH)) return;
    if (pluginConfFile.isConfigurationSection(MARKER_SETS_PATH)) return;

    List<String> flatSets = pluginConfFile.getStringList(MARKER_SETS_PATH);

    Map<String, Object> levelled = new LinkedHashMap<>();
    for (String markerSetId : flatSets) {
      levelled.put(markerSetId, DEFAULT_MARKER_SET_LEVEL);
    }

    pluginConfFile.set(MARKER_SETS_PATH, levelled);
    savePluginConfFile();

    if (!flatSets.isEmpty()) {
      Log.info("Converted " + flatSets.size() + " marker set(s) to the levelled format at "
          + "level " + DEFAULT_MARKER_SET_LEVEL + ".");
    }
  }

  /**
   * @method getMarkerSetLevels - Every configured marker set with the level it sits at.
   * @return An insertion-ordered map of marker set id to level. Sets with no explicit
   *     level fall back to DEFAULT_MARKER_SET_LEVEL.
   */
  public static Map<String, Integer> getMarkerSetLevels() {
    Map<String, Integer> levels = new LinkedHashMap<>();

    ConfigurationSection section = pluginConfFile.getConfigurationSection(MARKER_SETS_PATH);
    if (section == null) return levels;

    for (String markerSetId : section.getKeys(false)) {
      // A key with no value is a set the admin has not levelled yet.
      levels.put(markerSetId, section.getInt(markerSetId, DEFAULT_MARKER_SET_LEVEL));
    }

    return levels;
  }

  /**
   * @method getMarkerSets - Get the ids of every configured marker set.
   * @return {List<String>}
   */
  public static List<String> getMarkerSets() {
    return new ArrayList<>( getMarkerSetLevels().keySet() );
  }

  /**
   * @method getMarkerSetsByLevel - Configured marker sets grouped by level, broadest first.
   * @return A level-ordered map of level to the marker set ids at that level.
   */
  public static SortedMap<Integer, List<String>> getMarkerSetsByLevel() {
    SortedMap<Integer, List<String>> byLevel = new TreeMap<>();

    for (Map.Entry<String, Integer> entry : getMarkerSetLevels().entrySet()) {
      byLevel.computeIfAbsent( entry.getValue(), level -> new ArrayList<>() )
          .add( entry.getKey() );
    }

    return byLevel;
  }

  /**
   * @method getMarkerSetLevel - The level a marker set sits at.
   * @param markerSetId The BlueMap marker set id.
   * @return Its level, or DEFAULT_MARKER_SET_LEVEL if it is not configured.
   */
  public static int getMarkerSetLevel(String markerSetId) {
    return getMarkerSetLevels().getOrDefault( markerSetId, DEFAULT_MARKER_SET_LEVEL );
  }

  /**
   * @method setMarkerSetLevel - Move an already configured marker set to a level.
   * @param markerSetId The BlueMap marker set id.
   * @param level The level to move it to.
   * @return true if it was moved, false if the set is not configured.
   */
  public static boolean setMarkerSetLevel(String markerSetId, int level) {
    if (!getMarkerSetLevels().containsKey( markerSetId )) return false;

    pluginConfFile.set(MARKER_SETS_PATH + "." + markerSetId, level);
    savePluginConfFile();

    return true;
  }

  /**
   * @method addMarkerSet - Add a marker set id at a level and persist it.
   * @param markerSetId The BlueMap marker set id to add.
   * @param level The level to place it at.
   * @return true if it was added, false if it was already configured.
   */
  public static boolean addMarkerSet(String markerSetId, int level) {
    if (getMarkerSetLevels().containsKey( markerSetId )) return false;

    pluginConfFile.set(MARKER_SETS_PATH + "." + markerSetId, level);
    savePluginConfFile();

    return true;
  }

  /**
   * @method addMarkerSet - Add a marker set id at the default level.
   * @param markerSetId The BlueMap marker set id to add.
   * @return true if it was added, false if it was already configured.
   */
  public static boolean addMarkerSet(String markerSetId) {
    return addMarkerSet( markerSetId, DEFAULT_MARKER_SET_LEVEL );
  }

  /**
   * @method removeMarkerSet - Remove a marker set id from Maps.marker-sets and persist it.
   * @param markerSetId The BlueMap marker set id to remove.
   * @return true if it was removed, false if it was not configured.
   */
  public static boolean removeMarkerSet(String markerSetId) {
    if (!getMarkerSetLevels().containsKey( markerSetId )) return false;

    pluginConfFile.set(MARKER_SETS_PATH + "." + markerSetId, null);
    savePluginConfFile();

    return true;
  }

  /**
   * @method getPluginConfFile - Get the plugin configuration file.
   */
  public static FileConfiguration getPluginConfFile() {
    return pluginConfFile;
  }

  /**
   * @method savePluginConfFile - Save the plugin configuration file.
   */
  public static void savePluginConfFile() {
    try {
      pluginConfFile.save(confFile);
    }
    catch (IOException fileSaveErr) {
      Log.warning("Unable to save the plugin configuration!");
    }
  }

  /**
   * @method saveNoticeExclusionsConf - Save the notice exclusions configuration file.
   */
  public static void saveNoticeExclusionsConf() {
    try {
      noticeExclusionsConfFile.save(noticeExclusionsConf);
    }
    catch (IOException fileSaveErr) {
      Log.warning("Unable to save the notice exclusions configuration!");
    }
  }

  /**
   * @method reloadPluginConfgFile - Reload the plugin configuration files.
   */
  public static void reloadPluginConfigFile() {
    loadConfig();

    BMZ.movementHandler.reloadConfig();
  }

  /**
   * @method getNoticeType - Get the notice type stored for a player.
   * @param uuid The player's UUID.
   * @return The stored type, or NoticeType.DEFAULT if none is set or the stored
   *     value is not recognised.
   */
  public static NoticeType getNoticeType(UUID uuid) {
    String stored = noticeExclusionsConfFile.getString(NOTICES_PATH + "." + uuid);
    NoticeType type = NoticeType.fromId(stored);

    if (stored != null && type == null) {
      Log.warning("Unrecognised notice type '" + stored + "' for " + uuid
          + "; falling back to " + NoticeType.DEFAULT.getId() + ".");
    }

    return type == null ? NoticeType.DEFAULT : type;
  }

  /**
   * @method setNoticeType - Store the notice type for a player and persist it.
   * @param uuid The player's UUID.
   * @param type The type to store.
   */
  public static void setNoticeType(UUID uuid, NoticeType type) {
    noticeExclusionsConfFile.set(NOTICES_PATH + "." + uuid, type.getId());
    saveNoticeExclusionsConf();
  }

  /**
   * @method generateConfigComments - Generate the comments for the plugin configuration file.
   * @return {List<String>}
   */
  private static List<String> generateConfigComments() {
    List< String > comments = new ArrayList<>();
    comments.add( "BlueMap-Zones Configuration File" );
    comments.add( "This file contains the configuration settings for the BlueMap-Zones plugin." );
    comments.add( "Wilderness-Name: The title to appear when the player is outside all known marker areas." );
    comments.add( "Maps: The list of maps to load and their marker sets." );
    comments.add( "  name: The name of the map to load." );
    comments.add( "  marker-sets: The marker sets to load, mapped to their zone level." );
    comments.add( "    Level 1 is the broadest tier; higher numbers nest inside it." );
    comments.add( "    Marker sets sharing a level merge into one zone space." );
    comments.add( "    A set with no level defaults to level " + DEFAULT_MARKER_SET_LEVEL + "." );
    comments.add( "    A plain list of ids is still accepted and converted on load." );
    return comments;
  }
}
