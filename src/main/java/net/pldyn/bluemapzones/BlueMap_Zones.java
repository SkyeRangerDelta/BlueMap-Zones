package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import jdk.jfr.Description;
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
        int shapeCount = 0;
        for (Map.Entry<String, Marker> entry : setMarkers.entrySet()) {
            Log.info("Thinking about shape " + ++shapeCount + " of " + setMarkers.size());
            String key = entry.getKey();
            Marker value = entry.getValue();
            assert false;
            shapedChunks.add(catalogMarker(key, value, shapedChunks));
        }
    }

    private ShapedChunk catalogMarker(String k, Marker m, ArrayList<ShapedChunk> chunkList) {
        if (!(m instanceof ShapeMarker shapeMarker)) return null;
        Shape markerShape = shapeMarker.getShape();
        Vector2d[] markerPoints = markerShape.getPoints();
        ShapedChunk workingChunk = new ShapedChunk(shapeMarker);
        getServer().getPluginManager().registerEvents(workingChunk, this);
        int vCount = 0;

        Log.info("Processing " + shapeMarker.getLabel() + " with " + markerPoints.length + " vertex point(s).");
        for (Vector2d vertex : markerPoints) {
            int chID_X = vertex.getFloorX() / 16;
            int chID_Y = vertex.getFloorY() / 16;

            Vector2d chID = new Vector2d(chID_X, chID_Y);

            //Skip if ID already listed
            if (workingChunk.getChunks().contains(chID)) continue;

            Log.info("Adding chunk ID (" + chID_X + ", " + chID_Y + ").");
            workingChunk.addChunk(chID);
        }
        Log.info(shapeMarker.getLabel() + " has " + workingChunk.getChunks().size() + " chunks.");

        return workingChunk;
    }

    @Description("Returns a the ShapedChunk if it shares contains the tested Chunk ID")
    private boolean conflictedChunk(Vector2d chunkID, ArrayList<ShapedChunk> shapedChunks) {
        for (ShapedChunk shapedChunk : shapedChunks) {
            if (shapedChunk.getChunks().contains(chunkID)) {
                Log.info("Detected chunk conflict with " + shapedChunk.getShape().getLabel());
                return true;
            };
        }

        return false;
    }
}
