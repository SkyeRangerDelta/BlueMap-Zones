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
    pluginConfFile.addDefault("Maps.name", "world");
    pluginConfFile.addDefault("Maps.marker-set", "myMarkers");
    pluginConfFile.addDefault("Wilderness-Name", "Wilderness");
    savePluginConfFile();

    noticeExclusionsConfFile.addDefault("Exclusions", new ArrayList<>());
    saveNoticeExclusionsConf();
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
   * @method addNoticeExclusion - Add a UUID to the notice exclusions list.
   * @param uuid
   */
  public static void addNoticeExclusion(UUID uuid) {
    List<String> exclusions = noticeExclusionsConfFile.getStringList("Exclusions");
    exclusions.add(uuid.toString());
    noticeExclusionsConfFile.set("Exclusions", exclusions);
    saveNoticeExclusionsConf();
  }

  /**
   * @method removeNoticeExclusion - Remove a UUID from the notice exclusions list.
   * @param uuid
   */
  public static void removeNoticeExclusion(UUID uuid) {
    List<String> exclusions = noticeExclusionsConfFile.getStringList("Exclusions");
    exclusions.remove(uuid.toString());
    noticeExclusionsConfFile.set("Exclusions", exclusions);
    saveNoticeExclusionsConf();
  }

  /**
   * @method getNoticeExclusions - Get the list of UUIDs to exclude from notices.
   * @return {List<UUID>}
   */
  public static List<UUID> getNoticeExclusions() {
    List<String> exclusions = noticeExclusionsConfFile.getStringList("Exclusions");
    List<UUID> uuidList = new ArrayList<>();
    for (String exclusion : exclusions) {
      uuidList.add(UUID.fromString(exclusion));
    }
    return uuidList;
  }
}
