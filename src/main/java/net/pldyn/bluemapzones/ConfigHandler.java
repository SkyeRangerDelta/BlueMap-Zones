package net.pldyn.bluemapzones;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class ConfigHandler {
    private static final Logger Log = Logger.getLogger("BM Zones");
    private static File confFile;
    private static FileConfiguration pluginConfFile;

    public static void confInit() {
        fileInit();
        pluginConfFile.options().copyDefaults(true);
        savePluginConfFile();
    }

    private static void fileInit() {
        confFile = new File(
                Bukkit.getPluginManager()
                        .getPlugin("BlueMap-Zones")
                        .getDataFolder(), "BM-ZonesConfig.yml");

        if (!confFile.exists()) {
            try {
                confFile.createNewFile();
            }
            catch (IOException fileCreationErr) {
                Log.warning("Unable to create a config file on the server!\n" + fileCreationErr.getMessage());
            }
        }

        pluginConfFile = YamlConfiguration.loadConfiguration(confFile);
    }

    public static void createDefaults() {
        pluginConfFile.addDefault("Game-World", "world");
        savePluginConfFile();
    }

    public static FileConfiguration getPluginConfFile() {
        return pluginConfFile;
    }

    public static void savePluginConfFile() {
        try {
            pluginConfFile.save(confFile);
        }
        catch (IOException fileSaveErr) {
            Log.warning("Unable to save the plugin configuration!");
        }
    }

    public static void reloadPluginConfgFile() {
        pluginConfFile = YamlConfiguration.loadConfiguration(confFile);
    }
}
