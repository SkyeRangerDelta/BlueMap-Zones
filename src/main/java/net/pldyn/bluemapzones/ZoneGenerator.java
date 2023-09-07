package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ZoneGenerator {
    private static final Logger Log = Logger.getLogger("BM Zones");
    private static final ArrayList<ZonedShape> zonedShapes = new ArrayList<>();
    public ZoneGenerator(BlueMapAPI blueMapAPI) {
        Log.info("API loaded.");
        BlueMapMap workingMap = findConfMaps(blueMapAPI.getMaps());

        if (workingMap == null) {
            Log.warning("Couldn't find the map to load!");
            return;
        }

        MarkerSet objectiveSet = findMarkerSets(workingMap);
        if (objectiveSet == null) {
            Log.warning("Couldn't find the marker set to load!");
            return;
        }

        handleMarkerSet(objectiveSet);
    }

    public ArrayList<ZonedShape> getZonedShapes() {
        return zonedShapes;
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
        Map<String, Marker> setMarkers = markerSet.getMarkers();
        Log.info("Cataloging " + setMarkers.size() + " markers.");
        int shapeCount = 0;
        for (Map.Entry<String, Marker> entry : setMarkers.entrySet()) {
            Log.info("Thinking about shape " + ++shapeCount + " of " + setMarkers.size());
            String key = entry.getKey();
            Marker value = entry.getValue();
            assert false;
            zonedShapes.add(catalogMarker(key, value, zonedShapes));
        }
    }

    private ZonedShape catalogMarker(String k, Marker m, ArrayList<ZonedShape> chunkList) {
        if (!(m instanceof ShapeMarker shapeMarker)) return null;
        Shape markerShape = shapeMarker.getShape();
        Vector2d[] markerPoints = markerShape.getPoints();
        ZonedShape newZone = new ZonedShape(m.getLabel(), markerShape, ((ShapeMarker) m).getShapeY());

        int cCount = 0;
        ZonedChunk previousChunk = new ZonedChunk(new Vector2d(0, 0));

        Log.info("Processing " + newZone.getLabel() + " with " + markerPoints.length
                + " vertex point(s).");
        for (Vector2d vertex : markerPoints) {
            int chID_X = vertex.getFloorX() / 16;
            int chID_Y = vertex.getFloorY() / 16;

            Vector2d chID = new Vector2d(chID_X, chID_Y);

            //Are we in a new chunk?
            if (chID.equals(previousChunk.getChunkId())) continue;

            //Skip if ID already listed
            if (newZone.isOwnedChunk(chID)) continue;

            ZonedChunk newChunk = new ZonedChunk(chID);

            //Is conflicted also owned by another shape?
            ArrayList<ZonedShape> conflictedOwners = conflictedChunk(chID);
            if (!conflictedOwners.isEmpty()) {
                for (ZonedShape owner : conflictedOwners) {
                    newChunk = owner.getOwnedChunks().get(chID);
                    newChunk.setConflicted(true);
                }
            };

            newChunk.addOwner(newZone);
            newChunk.setBoundary(true);
            newZone.addOwnedChunk(chID, newChunk);

            if (newChunk.isConflicted()) {
                Log.info("Adding conflicted chunk ID (" + chID_X + ", " + chID_Y + ")");
            }
            else {
                Log.info("Adding chunk ID (" + chID_X + ", " + chID_Y + ")");
            }

            previousChunk = newChunk;
        }
        Log.info(newZone.getLabel() + " has " + newZone.getOwnedChunks().size() + " chunks and "
                + cCount + " conflicted chunks.");

        return newZone;
    }

    private void generateShapeInteriors() {
        //Algo for filling hollow space
        for (ZonedShape shape : zonedShapes) {
            Vector2d chunkMax = shape.getMaxChunk();
            Vector2d chunkMin = shape.getMinChunk();

            HashMap<Vector2d, ZonedChunk> knownChunks = shape.getOwnedChunks();

            //Iterate across X chunks inside shape
            for (int x = chunkMin.getFloorX(); x <= chunkMax.getFloorX(); x++) {

                //Iterate across Z chunks inside shape
                for (int z = chunkMin.getFloorY(); z <= chunkMax.getFloorY(); z++) {
                    Vector2d testId = new Vector2d(x, z);
                    boolean insideShape = false;

                    //Chunk is already known
                    if (knownChunks.containsKey(testId)) continue;

                    //Axis check each chunk within the bounds of the shape
                    //external chunks won't be able to hit all sides once
                    //Check North
                    if (!checkNorth(chunkMax, chunkMin, knownChunks, testId)) continue;

                    //Check West
                    if (!checkWest(chunkMax, chunkMin, knownChunks)) continue;

                    //Check South
                    if (!checkSouth(chunkMax, chunkMin, knownChunks)) continue;

                    //Check East
                    if (!checkEast(chunkMax, chunkMin, knownChunks)) continue;

                }
            }
        }
    }

    private boolean checkNorth(Vector2d chunkMax, Vector2d chunkMin, HashMap<Vector2d,
            ZonedChunk> knownChunks, Vector2d testID) {

        int boundaryIntersections = 0;

        boolean boundarySegment = false;

        Vector2d adjChunkId = new Vector2d(testID.getFloorX(), testID.getFloorY() - 1);

        if (knownChunks.containsKey(adjChunkId)) {
            ZonedChunk adjChunk = knownChunks.get(adjChunkId);
            if (adjChunk.isBoundary() && !boundarySegment) {
                boundarySegment = true;
                boundaryIntersections++;
            }
        }

        return false;
    }

    private boolean checkWest(Vector2d chunkMax, Vector2d chunkMin, HashMap<Vector2d,
            ZonedChunk> knownChunks, Vector2d testID) {

        int boundaryIntersections = 0;

        boolean boundarySegment = false;

        Vector2d adjChunkId = new Vector2d(testID.getFloorX(), testID.getFloorY() - 1);

        if (knownChunks.containsKey(adjChunkId)) {
            ZonedChunk adjChunk = knownChunks.get(adjChunkId);
            if (adjChunk.isBoundary() && !boundarySegment) {
                boundarySegment = true;
                boundaryIntersections++;
            }
        }

        return false;
    }

    private boolean checkSouth(Vector2d chunkMax, Vector2d chunkMin, HashMap<Vector2d,
            ZonedChunk> knownChunks, Vector2d testID) {

        int boundaryIntersections = 0;

        boolean boundarySegment = false;

        Vector2d adjChunkId = new Vector2d(testID.getFloorX(), testID.getFloorY() - 1);

        if (knownChunks.containsKey(adjChunkId)) {
            ZonedChunk adjChunk = knownChunks.get(adjChunkId);
            if (adjChunk.isBoundary() && !boundarySegment) {
                boundarySegment = true;
                boundaryIntersections++;
            }
        }
        return false;
    }

    private boolean checkEast(Vector2d chunkMax, Vector2d chunkMin, HashMap<Vector2d,
            ZonedChunk> knownChunks, Vector2d testID) {

        int boundaryIntersections = 0;

        boolean boundarySegment = false;

        Vector2d adjChunkId = new Vector2d(testID.getFloorX(), testID.getFloorY() - 1);

        if (knownChunks.containsKey(adjChunkId)) {
            ZonedChunk adjChunk = knownChunks.get(adjChunkId);
            if (adjChunk.isBoundary() && !boundarySegment) {
                boundarySegment = true;
                boundaryIntersections++;
            }
        }

        return false;
    }

    private ArrayList<ZonedShape> conflictedChunk(Vector2d zonedChunkId) {
        ArrayList<ZonedShape> conflictedOwners = new ArrayList<>();
        for (ZonedShape zonedShape : zonedShapes) {
            HashMap<Vector2d, ZonedChunk> ownedChunks = zonedShape.getOwnedChunks();
            if (ownedChunks.containsKey(zonedChunkId)) {
                Log.info("Detected chunk conflict with " + zonedShape.getLabel());
                conflictedOwners.add(zonedShape);
            };
        }

        return conflictedOwners;
    }
}
