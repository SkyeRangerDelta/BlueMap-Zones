package net.pldyn.bluemapzones;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import net.pldyn.bluemapzones.commands.generateCommand;
import net.pldyn.bluemapzones.commands.markerSetCommand;
import net.pldyn.bluemapzones.commands.reloadConfCommand;
import net.pldyn.bluemapzones.commands.toggleNoticeCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

public final class BlueMap_Zones extends JavaPlugin {

  private static final Logger Log = Logger.getLogger("BM Zones");
  private java.util.UUID UUID;

  public MovementHandler movementHandler;
  public ToolHandler toolHandler;

  private BlueMapAPI bma;
  private static BlueMap_Zones BMZ;

  private ArrayList<ZonedShape> zonedShapes = new ArrayList<>();
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

    generateCommand generate = new generateCommand();
    Objects.requireNonNull(
      getCommand( "bmz-generate" ) )
      .setExecutor( generate );
    Objects.requireNonNull(
      getCommand( "bmz-generate" ) )
      .setTabCompleter( generate );

    toggleNoticeCommand toggleNotices = new toggleNoticeCommand();
    Objects.requireNonNull(
      getCommand( "bmz-toggle-notices" ) )
      .setExecutor( toggleNotices );
    Objects.requireNonNull(
      getCommand( "bmz-toggle-notices" ) )
      .setTabCompleter( toggleNotices );

    reloadConfCommand reloadConf = new reloadConfCommand();
    Objects.requireNonNull(
      getCommand( "bmz-reload-conf" ) )
      .setExecutor( reloadConf );
    Objects.requireNonNull(
      getCommand( "bmz-reload-conf" ) )
      .setTabCompleter( reloadConf );

    markerSetCommand markerSets = new markerSetCommand();
    Objects.requireNonNull(
      getCommand( "bmz-markerset" ) )
      .setExecutor( markerSets );
    Objects.requireNonNull(
      getCommand( "bmz-markerset" ) )
      .setTabCompleter( markerSets );

    getServer().getPluginManager().registerEvents(movementHandler, this);
    getServer().getPluginManager().registerEvents(toolHandler, this);

    generateZones();

    Log.info("Plugin initialized!");
  }

//  @Override
//  public void onZoneGenerationComplete(ArrayList<ZonedShape> zonedShapes) {
//    runningGeneration = false;
//    setZonedShapes(zonedShapes);
//    Log.info("Zone generation complete!");
//    Log.info("Generated " + zonedShapes.size() + " zones.");
//  }

  public void generateZones() {
    // Deliberately does not clear the live zones here. The existing zones stay
    // usable until the new generation finishes and hands over a replacement.
    runningGeneration = true;
    new ZoneGenerator(bma, this).start();
    Log.info("Zone generation started!");
  }

  /**
   * @method setZonedShapes - Publish a completed generation to everything that reads zones.
   * @param newShapes The shapes the generator produced.
   */
  public void setZonedShapes(ArrayList<ZonedShape> newShapes) {
    // Copy rather than alias. The generator owns its own list, and previously
    // this method cleared the very list it was being handed, wiping every
    // regeneration after the first.
    this.zonedShapes = new ArrayList<>( newShapes );

    movementHandler.setZonedShapes( this.zonedShapes );
    toolHandler.setZonedShapes( this.zonedShapes );
  }

  public BlueMapAPI getBlueMapAPI() {
    return bma;
  }

  /**
   * @method getConfiguredMap - Resolve the BlueMap map named by Maps.name in the config.
   * @return The matching map, or null if BlueMap is not ready yet or no map has that id.
   */
  public BlueMapMap getConfiguredMap() {
    if (bma == null) return null;

    String confMap = (String) ConfigHandler.getPluginConfFile().get( "Maps.name" );
    for (BlueMapMap map : bma.getMaps()) {
      if (map.getId().equals(confMap)) return map;
    }

    return null;
  }

  public static BlueMap_Zones getInstance() {
    return BMZ;
  }

  public void setGenerating(boolean generating) {
    runningGeneration = generating;
  }

  public boolean isGenerating() {
    return runningGeneration;
  }
}
