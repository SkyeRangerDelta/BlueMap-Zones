package net.pldyn.bluemapzones;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class ConfigHandler {
  private static final Logger Log = Logger.getLogger("BM Zones");

  /** Section in BMZ-NoticeExclusions.yml mapping a player UUID to a NoticeType id. */
  private static final String NOTICES_PATH = "Notices";

  /** List in BMZ-Config.yml naming the BlueMap marker sets to build zones from. */
  private static final String MARKER_SETS_PATH = "Maps.marker-sets";

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

    migrateNoticeExclusions();
    saveNoticeExclusionsConf();
  }

  /**
   * @method migrateNoticeExclusions - Convert the legacy flat "Exclusions" list of UUIDs
   *     into the "Notices" section, which maps each UUID to a notice type. Players who had
   *     opted out become NoticeType.OFF; everyone else falls through to NoticeType.DEFAULT.
   *     Safe to call repeatedly - it is a no-op once "Exclusions" is gone.
   */
  private static void migrateNoticeExclusions() {
    if (!noticeExclusionsConfFile.contains("Exclusions")) return;

    List<String> legacyExclusions = noticeExclusionsConfFile.getStringList("Exclusions");

    for (String uuid : legacyExclusions) {
      // Do not clobber a preference that already exists in the new format.
      if (noticeExclusionsConfFile.contains(NOTICES_PATH + "." + uuid)) continue;
      noticeExclusionsConfFile.set(NOTICES_PATH + "." + uuid, NoticeType.OFF.getId());
    }

    noticeExclusionsConfFile.set("Exclusions", null);

    if (!legacyExclusions.isEmpty()) {
      Log.info("Migrated " + legacyExclusions.size() + " notice exclusion(s) to the "
          + NOTICES_PATH + " format.");
    }
  }

  /**
   * @method getMarkerSets - Get the list of marker sets for the plugin.
   * @return {List<String>}
   */
  public static List<String> getMarkerSets() {
    return pluginConfFile.getStringList("Maps.marker-sets");
  }

  /**
   * @method addMarkerSet - Add a marker set id to Maps.marker-sets and persist it.
   * @param markerSetId The BlueMap marker set id to add.
   * @return true if it was added, false if it was already configured.
   */
  public static boolean addMarkerSet(String markerSetId) {
    List<String> markerSets = getMarkerSets();
    if (markerSets.contains(markerSetId)) return false;

    markerSets.add(markerSetId);
    pluginConfFile.set(MARKER_SETS_PATH, markerSets);
    savePluginConfFile();

    return true;
  }

  /**
   * @method removeMarkerSet - Remove a marker set id from Maps.marker-sets and persist it.
   * @param markerSetId The BlueMap marker set id to remove.
   * @return true if it was removed, false if it was not configured.
   */
  public static boolean removeMarkerSet(String markerSetId) {
    List<String> markerSets = getMarkerSets();
    if (!markerSets.remove(markerSetId)) return false;

    pluginConfFile.set(MARKER_SETS_PATH, markerSets);
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
    comments.add( "  marker-sets: The list of marker sets to load for the map." );
    return comments;
  }
}
