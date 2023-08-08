package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
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
        BlueMapMap workingMap = findConfMaps(blueMapAPI.getMaps());

        if (workingMap == null) {
            Log.warning("Couldn't find the map to load!");
            return;
        }

        MarkerSet objectiveSet = findMarkerSets(workingMap);
        if (objectiveSet == null) {
            Log.warning("Couldnt find the marker set to load!");
            return;
        }

        handleMarkerSet(objectiveSet);
    }

    private BlueMapMap findConfMaps(Collection<BlueMapMap> loadedWorlds) {
        String confWorld = (String) ConfigHandler.getPluginConfFile().get("Maps.name");
        for (BlueMapMap m : loadedWorlds) {
            if (m.getId().equals(confWorld)) {
                Log.info("Found an operating world. (" + m.getName() + ")");
                return m;
            }
        }

        return null;
    }

    private MarkerSet findMarkerSets(BlueMapMap world) {
        String markerID = (String) ConfigHandler.getPluginConfFile().get("Maps.marker-set");
        Map<String, MarkerSet> markerSets = world.getMarkerSets();
        Log.info("World has " + markerSets.size() + " marker sets.");

        if (markerSets.isEmpty()) {
            Log.info("Map has no marker sets!");
            return null;
        }

        if (!markerSets.containsKey(markerID)) {
            Log.info("Marker set requested is not available!");
            return null;
        }

        MarkerSet mSet = markerSets.get(markerID);

        if (mSet != null) Log.info("Found the configured marker set. (" + mSet.getLabel() + ")");
        return mSet;
    }

    private void handleMarkerSet(MarkerSet markerSet) {
        ArrayList<ShapedChunk> shapedChunks = new ArrayList<>();
        Map<String, Marker> setMarkers = markerSet.getMarkers();
        Log.info("Catalogging " + setMarkers.size() + " markers.");
        for (Map.Entry<String, Marker> entry : setMarkers.entrySet()) {
            String key = entry.getKey();
            Marker value = entry.getValue();
            assert false;
            shapedChunks.add(catalogMarker(key, value));
        }
    }

    private ShapedChunk catalogMarker(String k, Marker m) {
        if (!(m instanceof ShapeMarker shapeMarker)) return null;
        Shape markerShape = shapeMarker.getShape();
        Vector2d[] markerPoints = markerShape.getPoints();
        ShapedChunk workingChunk = new ShapedChunk(shapeMarker);
        getServer().getPluginManager().registerEvents(workingChunk, this);

        Log.info("Processing " + shapeMarker.getLabel() + " with " + markerPoints.length + " vertex point(s).");
        Vector2d shapeMax = markerShape.getMax();
        Vector2d shapeMin = markerShape.getMin();
        Log.info(shapeMarker.getLabel() + " has max (" + shapeMax.getX() + ", " + shapeMax.getY() + ").");
        Log.info(shapeMarker.getLabel() + " has min (" + shapeMin.getX() + ", " + shapeMin.getY() + ").");

        //Collect the number of chunks (rounded down) that the shape covers.
        int shapeMaxX = (int) Math.round(Math.floor(shapeMax.getX()));
        int shapeMaxZ = (int) Math.round(Math.floor(shapeMax.getY()));
        int shapeMinX = (int) Math.round(Math.floor(shapeMin.getX()));
        int shapeMinZ = (int) Math.round(Math.floor(shapeMin.getY()));

        int shapeCellsX = Math.floorDiv((shapeMaxX - shapeMinX), 16);
        int shapeCellsZ = Math.floorDiv((shapeMaxZ - shapeMinZ), 16);

        //Iterate through each chunk determining the boundaries based on position
        for (int cX = 0; cX < shapeCellsX; cX++) {
            for (int cZ = 0; cZ < shapeCellsZ; cZ++) {
                double chunkMinX = shapeMin.getX() + (cX * 16);
                double chunkMaxX = shapeMax.getX() + ((cX + 1) * 16);
                double chunkMinZ = shapeMin.getY() + (cZ * 16);
                double chunkMaxZ = shapeMax.getY() + ((cZ + 1) * 16);

                //If the chunk contains the edge of the shape
                if (chunkMaxX >= shapeMin.getX() && chunkMinX <= shapeMax.getX() &&
                        chunkMaxZ >= shapeMin.getY() && chunkMinZ <= shapeMax.getY()) {
                    int chunkX = Math.floorDiv(cX, 16);
                    int chunkY = Math.floorDiv(cZ, 16);
                    Log.info("Adding chunk ID (" + chunkX + ", " + chunkY + ").");
                    Vector2d chunkID = new Vector2d(chunkX, chunkY);
                    workingChunk.addChunk(chunkID);
                }
            }
        }

        Log.info(shapeMarker.getLabel() + " has " + workingChunk.getChunks().size() + " chunks.");

        return workingChunk;
    }
}
