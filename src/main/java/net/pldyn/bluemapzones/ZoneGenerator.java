package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.*;
import java.util.logging.Logger;

public class ZoneGenerator extends Thread {
    private static final Logger Log = Logger.getLogger("BM Zones");
    private static final ArrayList<ZonedShape> zonedShapes = new ArrayList<>();
    private final BlueMapAPI blueMapAPI;
    private final BlueMap_Zones plugin = BlueMap_Zones.getInstance();
    public ZoneGenerator(BlueMapAPI blueMapApi) {
        this.blueMapAPI = blueMapApi;
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

        Log.info("Processing " + newZone.getLabel() + " with " + markerPoints.length
                + " vertex point(s).");

        ZonedShape newZone2 = buildShapeBoundary(markerPoints, newZone);

        int cCount = newZone2.getConflictedChunks().size();

        Log.info(newZone.getLabel() + " has " + newZone.getOwnedChunks().size() + " boundary chunks and "
                + cCount + " conflicted boundary chunks.");

        return newZone2;
    }

    private ZonedShape buildShapeBoundary(Vector2d[] markerPoints, ZonedShape newZone) {
        Vector2d prevChunk = null;
        ArrayList<Vector2d> shapeChunks = new ArrayList<>();
        ArrayList<Vector2d> bresenhamChunks = new ArrayList<>();

        //Build all chunk IDs
        for (Vector2d markerPoint : markerPoints) {
            Vector2d markerChunkId = new Vector2d(Math.floorDiv(markerPoint.getFloorX(), 16),
                    Math.floorDiv(markerPoint.getFloorY(), 16));

            shapeChunks.add(markerChunkId);
        }

        //Iterate across chunks and do magic
        for (Vector2d chunkId : shapeChunks) {
            if (prevChunk == null) {
                ZonedChunk newChunk = addChunk(new ZonedChunk(chunkId), chunkId, newZone);
                prevChunk = chunkId;

                newZone.addOwnedChunk(chunkId, newChunk);
                continue;
            }

            if (chunkId == prevChunk) continue;
            if (newZone.isOwnedChunk(chunkId)) {
                prevChunk = chunkId;
                continue;
            }

            if (!isAdjacent(prevChunk, chunkId)) {
                //Run bresenham for non-adjacent chunks
                bresenhamChunks.addAll(doBresenham(prevChunk, chunkId));
            }

            ZonedChunk newChunk = addChunk(new ZonedChunk(chunkId), chunkId, newZone);
            newZone.addOwnedChunk(chunkId, newChunk);

            prevChunk = chunkId;
        }

        for (Vector2d bresenhamChunk : bresenhamChunks) {
            if (newZone.isOwnedChunk(bresenhamChunk)) continue;

            ZonedChunk bresenhamId = addChunk(new ZonedChunk(bresenhamChunk), bresenhamChunk, newZone);
            newZone.addOwnedChunk(bresenhamChunk, bresenhamId);
        }

        return newZone;
    }

    private ZonedChunk addChunk(ZonedChunk newChunk, Vector2d chId, ZonedShape newZone) {
        //Is conflicted also owned by another shape?
        ArrayList<ZonedShape> conflictedOwners = conflictedChunk(chId);
        if (!conflictedOwners.isEmpty()) {
            for (ZonedShape owner : conflictedOwners) {
                newChunk = owner.getOwnedChunks().get(chId);
                newChunk.setConflicted(true);
            }
        };

        newChunk.addOwner(newZone);
        newChunk.setBoundary(true);

        if (newChunk.isConflicted()) {
            Log.info("Adding conflicted chunk ID (" + chId.getFloorX() + ", " + chId.getFloorY() + ")");
        }
        else {
            Log.info("Adding chunk ID (" + chId.getFloorX() + ", " + chId.getFloorY() + ")");
        }

        return newChunk;
    }

    private boolean isAdjacent(Vector2d lastChunkId, Vector2d testId) {
        //Check E, W, S, N (in order) for immediate or diagonal adjacency
        int prevX = lastChunkId.getFloorX();
        int prevZ = lastChunkId.getFloorY();
        int testX = testId.getFloorX();
        int testZ = testId.getFloorY();

        return (testX + 1 == prevX && testZ == prevZ) ||    //E
                (testX - 1 == prevX && testZ == prevZ) ||   //W
                (testX == prevX && testZ + 1 == prevZ) ||   //S
                (testX == prevX && testZ - 1 == prevZ) ||   //N
                (testX + 1 == prevX && testZ + 1 == prevZ) ||   //SE
                (testX + 1 == prevX && testZ - 1 == prevZ) ||   //NE
                (testX - 1 == prevX && testZ + 1 == prevZ) ||   //SW
                (testX - 1 == prevX && testZ - 1 == prevZ); //NW
    }

    private ArrayList<Vector2d> doBresenham(Vector2d lastChunkId, Vector2d nextChunkId) {
        //Run Bresenham Line to retrofill gaps in boundary segments
        int x1 = lastChunkId.getFloorX();
        int x2 = nextChunkId.getFloorX();
        int z1 = lastChunkId.getFloorY();
        int z2 = nextChunkId.getFloorY();

        ArrayList<Vector2d> lineIds = new ArrayList<>();

        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);

        int sX = Integer.signum(x2 - x1);
        int sZ = Integer.signum(z2 - z1);

        boolean flip = false;
        if (dz > dx) {
            flip = true;
            int temp = dx;
            dx = dz;
            dz = temp;
        }

        int decision = 2 * dz - dx;

        int workingX = x1;
        int workingZ = z1;

        for (int i = 0; i < dx; i++) {
            if (decision >= 0) {
                if (flip) {
                    workingX += sX;
                }
                else {
                   workingZ += sZ;
                }

                decision -= 2 * dx;
            }

            if (flip) {
                workingZ += sZ;
            }
            else {
                workingX += sX;
            }

            decision += 2 * dx;

            //Testing coord
            Vector2d id = new Vector2d(workingX, workingZ);
            if (!lineIds.contains(id)) {
                Log.info("Adding Bresenham ID (" + workingX + ", " + workingZ + ").");
                lineIds.add(id);
            }
        }

        return lineIds;
    }

    private void generateShapeInteriors() {
        for (ZonedShape shape : zonedShapes) {
            shape.doInteriorGeneration();
        }
    }

    private ArrayList<ZonedShape> conflictedChunk(Vector2d zonedChunkId) {
        ArrayList<ZonedShape> conflictedOwners = new ArrayList<>();
        for (ZonedShape zonedShape : zonedShapes) {
            HashMap<Vector2d, ZonedChunk> ownedChunks = zonedShape.getOwnedChunks();
            if (ownedChunks.containsKey(zonedChunkId)) {
                conflictedOwners.add(zonedShape);
            };
        }

        return conflictedOwners;
    }

    public void run() {
        Log.info("Starting child thread generator.");

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

        //Build shapes and their bounds
        handleMarkerSet(objectiveSet);

        //Build shape interiors
        generateShapeInteriors();

        plugin.setZonedShapes(zonedShapes);
        plugin.setGenerating(false);
    }
}
