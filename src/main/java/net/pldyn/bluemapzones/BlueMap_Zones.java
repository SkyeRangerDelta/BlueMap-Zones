package net.pldyn.bluemapzones;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public final class BlueMap_Zones extends JavaPlugin {

    private static final Logger Log = Logger.getLogger("BM Zones");

    @Override
    public void onEnable() {
        ConfigHandler.confInit();
        ConfigHandler.createDefaults();

        Log.info("Plugin enabled!");
        BlueMapAPI.onEnable(this::initPlugin);
    }

    @Override
    public void onDisable() {
        Log.info("Plugin halted!");
    }

    private void initPlugin(BlueMapAPI blueMapAPI) {
        Log.info("API loaded.");
        BlueMapMap workingWorld = findConfWorlds(blueMapAPI.getMaps());
    }

    private BlueMapMap findConfWorlds(Collection<BlueMapMap> loadedWorlds) {
        String confWorld = (String) ConfigHandler.getPluginConfFile().get("Game-World");
        for (BlueMapMap m : loadedWorlds) {
            if (m.getId().equals(confWorld)) {
                Log.info("Found an operating world. (" + m.getId() + ")");
                return m;
            }
        }

        return null;
    }
}
