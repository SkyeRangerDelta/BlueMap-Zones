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

        //Build shapes and their bounds
        handleMarkerSet(objectiveSet);

        //Build shape interiors
        generateShapeInteriors();
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
        //Check E, W, S, N (in order) for immediate adjacency
        int prevX = lastChunkId.getFloorX();
        int prevZ = lastChunkId.getFloorY();
        int testX = testId.getFloorX();
        int testZ = testId.getFloorY();

        return (testX + 1 == prevX && testZ == prevZ) ||
                (testX - 1 == prevX && testZ == prevZ) ||
                (testX == prevX && testZ + 1 == prevZ) ||
                (testX == prevX && testZ - 1 == prevZ);
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
        int shapeC = 1;
        int cellC = 1;

        //Algo for filling hollow space
        for (ZonedShape shape : zonedShapes) {
            Log.info("Beginning shape interior build for shape " + shapeC + " of " + zonedShapes.size());
            shapeC++;

            Vector2d chunkMax = shape.getMaxChunk();
            Vector2d chunkMin = shape.getMinChunk();

            int shapeLength = chunkMax.getFloorX() - chunkMin.getFloorX();
            int shapeHeight = chunkMin.getFloorY() - chunkMin.getFloorY();

            int shapeCellCount = shapeLength * shapeHeight;

            Log.info("Shape has " + shapeCellCount + " (" + shapeLength + " * " + shapeHeight + ").");

            HashMap<Vector2d, ZonedChunk> knownChunks = shape.getOwnedChunks();

            //Iterate across Z chunks inside shape (N -> S)
            for (int z = chunkMin.getFloorY(); z <= chunkMax.getFloorY(); z++) {

                //Iterate across X chunks inside shape (W -> E)
                for (int x = chunkMin.getFloorX(); x <= chunkMax.getFloorX(); x++) {
                    Vector2d testId = new Vector2d(x, z);

                    Log.info("Testing chunk ID (" + testId.getFloorX() + ", " + testId.getFloorY() +
                            "). Chunk " + cellC + "/" + shapeCellCount);
                    cellC++;

                    //b4007438
                    //If chunk already known - skip
                    if (knownChunks.containsKey(testId)) continue;

                    //Test for inside?
                    if (isInsideShape(testId, chunkMax.getFloorX(), knownChunks)) {
                        ZonedChunk newChunk = new ZonedChunk(testId);
                        newChunk.addOwner(shape);
                        shape.addOwnedChunk(testId, new ZonedChunk(testId));
                    }
                }
            }
        }
    }

    private boolean isInsideShape(Vector2d testId, int xMax, HashMap<Vector2d,
            ZonedChunk> knownChunks) {

        boolean tangent = false;
        int boundCount = 0;
        boolean lastCellBoundary = false;

        //Iterating to the east (x++) via ray cast with tangent check
        for (int i = testId.getFloorX(); i <= xMax; i++) {
            if (knownChunks.containsKey(new Vector2d(i, testId.getFloorY()))) {
                //Contested cell is on the periphery
                if (lastCellBoundary) {
                    //Last checked cell was also a bound, possible tangent
                    tangent = true;
                }
                else {
                    //Crossed a bound
                    lastCellBoundary = true;
                    boundCount++;
                }
            }
            else {
                lastCellBoundary = false;
            }
        }

        //True if odd bound count / outside
        boolean castCheck = boundCount % 2 != 0;

        //If possible inside but there was a tangent
        //ie, glanced the side
        boolean isInside = !tangent || !castCheck;
        Log.info("Cell is " + (isInside ? "inside" : "outside") + " the shape.");

        return isInside;
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
