package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
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
            shapedChunks.add(catalogMarker(key, value, shapedChunks));
        }
    }

    private ShapedChunk catalogMarker(String k, Marker m, ArrayList<ShapedChunk> chunkList) {
        if (!(m instanceof ShapeMarker shapeMarker)) return null;
        Shape markerShape = shapeMarker.getShape();
        Vector2d[] markerPoints = markerShape.getPoints();
        ShapedChunk workingChunk = new ShapedChunk(shapeMarker);
        getServer().getPluginManager().registerEvents(workingChunk, this);

        Log.info("Processing " + shapeMarker.getLabel() + " with " + markerPoints.length + " vertex point(s).");
        Vector2d shapeMax = markerShape.getMax();
        Vector2d shapeMin = markerShape.getMin();

        //Collect the number of chunks (rounded down) that the shape covers.
        int shapeMaxX = (int) Math.floor(shapeMax.getX());
        int shapeMaxZ = (int) Math.floor(shapeMax.getY());
        int shapeMinX = (int) Math.floor(shapeMin.getX());
        int shapeMinZ = (int) Math.floor(shapeMin.getY());
        Log.info(shapeMarker.getLabel() + " has max (" + shapeMaxX + ", " + shapeMaxZ + ").");
        Log.info(shapeMarker.getLabel() + " has min (" + shapeMinX + ", " + shapeMinZ + ").");

        int shapeCellsX = (int) ((double) (shapeMaxX - shapeMinX) / 16);
        int shapeCellsZ = (int) ((double) (shapeMaxZ - shapeMinZ) / 16);
        Log.info("Shape covers " + shapeCellsX + " X chunks, and " + shapeCellsZ + " Z chunks.");

        //Iterate through each chunk determining the boundaries based on position
        for (int cX = 1; cX <= shapeCellsX; cX++) {
            for (int cZ = 1; cZ <= shapeCellsZ; cZ++) {
                Log.info("Testing X" + cX + ", Z" + cZ); //342, 84 - 371, 114
                int chunkMinX = (Math.floorDiv(shapeMinX, 16) * 16) + (cX * 16);
                int chunkMaxX = shapeMaxX + ((cX + 1) * 16);
                int chunkMinZ = (Math.floorDiv(shapeMinZ, 16) * 16) + (cZ * 16);
                int chunkMaxZ = shapeMaxZ + ((cZ + 1) * 16);

                //If the chunk contains the edge of the shape
                if (!(chunkMaxX >= shapeMinX && chunkMinX <= shapeMaxX &&
                        chunkMaxZ >= shapeMinZ && chunkMinZ <= shapeMaxZ)) continue;

                int chunkX = Math.floorDiv(chunkMaxX, 16);
                int chunkY = Math.floorDiv(chunkMaxZ, 16);

                Vector2d chunkID = new Vector2d(chunkX, chunkY);

                //Check for conflicted chunk, determine dom and use it instead
                ShapedChunk conflictingChunk = conflictedChunk(chunkID, chunkList);
                if (conflictingChunk != null) {
                    chunkID = determineDominateChunk(conflictingChunk, workingChunk, chunkID);
                    if (chunkID == null) {
                        Log.info("Contesting chunk failed to win");
                        continue;
                    }
                }

                Log.info("Adding chunk ID (" + chunkX + ", " + chunkY + ").");
                workingChunk.addChunk(chunkID);
            }
        }

        Log.info(shapeMarker.getLabel() + " has " + workingChunk.getChunks().size() + " chunks.");

        return workingChunk;
    }

    private ShapedChunk conflictedChunk(Vector2d chunkID, ArrayList<ShapedChunk> shapedChunks) {
        for (ShapedChunk shapedChunk : shapedChunks) {
            if (shapedChunk.getChunks().contains(chunkID)) {
                Log.info("Detected chunk conflict, altering indexes.");
                return shapedChunk;
            };
        }

        return null;
    }

    private Vector2d determineDominateChunk(ShapedChunk existingChunk, ShapedChunk newChunk, Vector2d chunkID) {
        ArrayList<Vector2d> existingChunkVertices = getChunkVertices(existingChunk, chunkID);
        ArrayList<Vector2d> newChunkVertices = getChunkVertices(newChunk, chunkID);

        //Older/processed chunk has no area - new chunk wins
        if (existingChunkVertices.size() < 3) return chunkID;

        //New chunk has no area - old chunk wins
        if (newChunkVertices.size() < 3) return null;

        double existingArea = determineShapeArea(existingChunkVertices);
        double newArea = determineShapeArea(newChunkVertices);

        //Old chunk area larger
        if (existingArea <= newArea) {
            Log.info("Existing chunk had larger area of " + existingArea);
            return null;
        }

        Log.info("New chunk has larger area of " + existingArea);

        //Otherwise, remove chunkID from existing chunk (lost) and return new chunk for addition
        ArrayList<Vector2d> existingChunkVerticesEdited = existingChunk.getChunks();
        existingChunkVerticesEdited.remove(chunkID);
        existingChunk.setChunks(existingChunkVerticesEdited);
        return chunkID;
    }

    private ArrayList<Vector2d> getChunkVertices(ShapedChunk newChunk, Vector2d chunkID) {
        ArrayList<Vector2d> vList = new ArrayList<>();
        for (Vector2d vertex : newChunk.getChunks()) {
            int vX = Math.floorDiv(vertex.getFloorX(), 16);
            int vZ = Math.floorDiv(vertex.getFloorY(), 16);
            if (vX == chunkID.getX() && vZ == chunkID.getY()) {
                vList.add(vertex);
            }
        }

        return vList;
    }

    private double determineShapeArea(ArrayList<Vector2d> vertexList) {
        double xySum = 0, yxSum = 0, vSum = 0;
        for (int i = 0; i < vertexList.size(); i++) {
            Vector2d vertex = vertexList.get(i);
            Vector2d nextVertex = vertexList.get(i+1);

            xySum += vertex.getX() * nextVertex.getY();
            yxSum += vertex.getY() * nextVertex.getX();
        }

        vSum = xySum + yxSum;
        return vSum / 2;
    }
}
