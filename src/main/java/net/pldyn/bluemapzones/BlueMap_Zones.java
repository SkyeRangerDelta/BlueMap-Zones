package net.pldyn.bluemapzones;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class BlueMap_Zones extends JavaPlugin {

    private static final Logger Log = Logger.getLogger("BM Zones");

    @Override
    public void onEnable() {
        Log.info("Plugin enabled!");
        BlueMapAPI.onEnable(this::initPlugin);
    }

    @Override
    public void onDisable() {
        Log.info("Plugin halted!");
    }

    public void initPlugin(BlueMapAPI blueMapAPI) {
        Log.info("API loaded.");
    }
}
