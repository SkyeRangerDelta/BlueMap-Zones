package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class ZonedShape extends ShapeMarker {

    private static final Logger Log = Logger.getLogger("BM Zones");
    private HashMap<Vector2d, ZonedChunk> ownedChunks = new HashMap<>();
    public Vector2d maxChunk = getMaxChunk();
    public Vector2d minChunk = getMinChunk();

    public ZonedShape(String label, Shape shape, float shapeY) {
        super(label, shape, shapeY);
        Log.info("Created a new zone shape.");
    }

    public HashMap<Vector2d, ZonedChunk> getOwnedChunks() {
        return ownedChunks;
    }

    public void setOwnedChunks(HashMap<Vector2d, ZonedChunk> ownedChunks) {
        this.ownedChunks = ownedChunks;
    }

    public void addOwnedChunk(Vector2d chunkId, ZonedChunk newOwnedChunk) {
        this.ownedChunks.put(chunkId, newOwnedChunk);
    }

    public boolean isOwnedChunk(Vector2d chunkId) {
        return ownedChunks.containsKey(chunkId);
    }

    public boolean isOwnedChunk(ZonedChunk ownedChunk) {
        return ownedChunks.containsValue(ownedChunk);
    }

    public Vector2d getMaxChunk() {
        Vector2d max = this.getShape().getMax();

        int maxX = Math.floorDiv(max.getFloorX(), 16);
        int maxZ = Math.floorDiv(max.getFloorY(), 16);

        return new Vector2d(maxX, maxZ);
    }

    public Vector2d getMinChunk() {
        Vector2d min = this.getShape().getMin();

        int minX = Math.floorDiv(min.getFloorX(), 16);
        int minZ = Math.floorDiv(min.getFloorY(), 16);

        return new Vector2d(minX, minZ);
    }

    public HashMap<Vector2d, ZonedChunk> getConflictedChunks() {
        HashMap<Vector2d, ZonedChunk> conflictedChunks = new HashMap<>();
        for (ZonedChunk chunk : ownedChunks.values()) {
            if (chunk.isConflicted()) conflictedChunks.put(chunk.getChunkId(), chunk);
        }

        return conflictedChunks;
    }

    public void doInteriorGeneration() {
        Vector2d chunkMax = getMaxChunk();
        Vector2d chunkMin = getMinChunk();

        int shapeLength = chunkMax.getFloorX() - chunkMin.getFloorX();
        int shapeHeight = chunkMax.getFloorY() - chunkMin.getFloorY();

        int shapeMaxPossibleCellCount = shapeLength * shapeHeight;
        Log.info("Shape has maximum cell count of " + shapeMaxPossibleCellCount);
        Vector2d startingId = null;

        //Target "inside" chunk - find *all* inside chunk possibilities
        for (int z = chunkMin.getFloorY(); z < chunkMax.getFloorY(); z++) {
            ArrayList<Vector2d> rowChunks = new ArrayList<>();
            ArrayList<Vector2d> rowSets = new ArrayList<>();
            for (Vector2d chunkId : ownedChunks.keySet()) {
                if (chunkId.getFloorY() != z) continue;
                rowChunks.add(chunkId);
            }

            for (Vector2d rowChunk : rowChunks) {
                if (rowChunks.contains(rowChunk.add(1, 0))) {
                    continue;
                }

                rowSets.add(rowChunk);
            }

            Log.info("Row chunks has " + rowChunks.size());
            if (rowChunks.size() == 2) {
                startingId = rowChunks.get(0).add(1, 0);
                break;
            }
        }

        Log.info("Found a starting ID at " + startingId);
        buildInterior(startingId);
    }

    private void buildInterior(Vector2d startingId) {
        if (startingId == null) return;
        if ((startingId.getFloorX() > maxChunk.getFloorX()) ||
            (startingId.getFloorX() < minChunk.getFloorX()) ||
            (startingId.getFloorY() > maxChunk.getFloorY()) ||
            (startingId.getFloorY() < minChunk.getFloorY())) return;

        if (!ownedChunks.containsKey(startingId)) {
            Log.info("Running interior build on chunk (" + startingId.getFloorX() +
                    ", " + startingId.getFloorY() + ")");
            ZonedChunk newChunk = new ZonedChunk(startingId);
            newChunk.addOwner(this);
            ownedChunks.put(startingId, newChunk);

            buildInterior(startingId.add(1, 0)); //E
            buildInterior(startingId.sub(1, 0)); //W
            buildInterior(startingId.add(0, 1)); //S
            buildInterior(startingId.sub(0, 1)); //N
        }
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
}
