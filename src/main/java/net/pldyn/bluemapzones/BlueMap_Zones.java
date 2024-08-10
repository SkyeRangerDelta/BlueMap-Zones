package net.pldyn.bluemapzones;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.pldyn.bluemapzones.commands.generateCommand;
import net.pldyn.bluemapzones.commands.reloadConfCommand;
import net.pldyn.bluemapzones.commands.toggleNoticeCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

public final class BlueMap_Zones extends JavaPlugin implements ZoneGenerationCallback {

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
    ConfigHandler.createPluginDefaults();

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

    Objects.requireNonNull(
        getCommand( "bmz-generate" ) )
        .setExecutor( new generateCommand() );

    Objects.requireNonNull(
            getCommand( "bmz-toggle-notices" ) )
        .setExecutor( new toggleNoticeCommand() );

    Objects.requireNonNull(
            getCommand( "bmz-reload-conf" ) )
        .setExecutor( new reloadConfCommand() );

    getServer().getPluginManager().registerEvents(movementHandler, this);
    getServer().getPluginManager().registerEvents(toolHandler, this);

    generateZones();

    Log.info("Plugin initialized!");
  }

  @Override
  public void onZoneGenerationComplete(ArrayList<ZonedShape> zonedShapes) {
    runningGeneration = false;
    setZonedShapes(zonedShapes);
    Log.info("Zone generation complete!");
    Log.info("Generated " + zonedShapes.size() + " zones.");

    movementHandler.setZonedShapes( zonedShapes );
  }

  public void generateZones() {
    runningGeneration = true;
    new ZoneGenerator(bma, this, this).start();
    Log.info("Zone generation started!");
  }

  public void setZonedShapes(ArrayList<ZonedShape> zonedShapes) {
    this.zonedShapes.clear();
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
