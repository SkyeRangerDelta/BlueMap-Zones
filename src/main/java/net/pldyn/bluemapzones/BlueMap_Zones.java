package net.pldyn.bluemapzones;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.pldyn.bluemapzones.commands.generateCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

public final class BlueMap_Zones extends JavaPlugin {

    private static final Logger Log = Logger.getLogger("BM Zones");
    private java.util.UUID UUID;
    public MovementHandler movementHandler;
    public ToolHandler toolHandler;
    private ArrayList<ZonedShape> zonedShapes = new ArrayList<>();
    private BlueMapAPI bma;
    private static BlueMap_Zones BMZ;
    private boolean runningGeneration = false;

    @Override
    public void onEnable() {
        BMZ = this;

        ConfigHandler.confInit();
        ConfigHandler.createDefaults();

        Log.info("Plugin enabled!");
        BlueMapAPI.onEnable(blueMapApi -> {
            bma = blueMapApi;
            doInit(blueMapApi);
        });
    }

    @Override
    public void onDisable() {
        Log.info("Plugin halted!");
    }

    private void doInit(BlueMapAPI blueMapAPI) {
        if (UUID != null) return;

        UUID = java.util.UUID.randomUUID();
        movementHandler = new MovementHandler(zonedShapes);
        toolHandler = new ToolHandler(zonedShapes);

        Objects.requireNonNull(getCommand("generate")).setExecutor(new generateCommand());

        getServer().getPluginManager().registerEvents(movementHandler, this);
        getServer().getPluginManager().registerEvents(toolHandler, this);
    }

    public void generateZones(BlueMapAPI blueMapAPI) {
        runningGeneration = true;
        new ZoneGenerator(blueMapAPI).start();
    }

    public void setZonedShapes(ArrayList<ZonedShape> zonedShapes) {
        this.zonedShapes = zonedShapes;
    }

    public BlueMapAPI getBlueMapAPI() {
        return bma;
    }

    public static BlueMap_Zones getInstance() {
        return BMZ;
    }

    public void setGenerating(boolean generating) {
        runningGeneration = generating;
    }
}
