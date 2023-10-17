package net.pldyn.bluemapzones;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public final class BlueMap_Zones extends JavaPlugin {

    private static final Logger Log = Logger.getLogger("BM Zones");
    private java.util.UUID UUID;
    public MovementHandler movementHandler;
    public ToolHandler toolHandler;

    @Override
    public void onEnable() {
        ConfigHandler.confInit();
        ConfigHandler.createDefaults();

        Log.info("Plugin enabled!");
        BlueMapAPI.onEnable(this::doInit);
    }

    @Override
    public void onDisable() {
        Log.info("Plugin halted!");
    }

    private void doInit(BlueMapAPI blueMapAPI) {
        if (UUID != null) return;

        UUID = java.util.UUID.randomUUID();
        ArrayList<ZonedShape> zonedShapes = new ZoneGenerator(blueMapAPI).getZonedShapes();
        movementHandler = new MovementHandler(zonedShapes);
        toolHandler = new ToolHandler(zonedShapes);
        getServer().getPluginManager().registerEvents(movementHandler, this);
        getServer().getPluginManager().registerEvents(toolHandler, this);
    }
}
