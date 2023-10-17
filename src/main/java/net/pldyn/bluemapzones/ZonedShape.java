package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

        Log.info("Starting interior generation on " + this.getLabel());

        Vector2d chunkMax = this.getMaxChunk();
        Vector2d chunkMin = this.getMinChunk();

        Vector2d startingId = null;
        boolean ranInterior = false;

        for (int z1 = chunkMin.getFloorY() + 1; z1 < chunkMax.getFloorY(); z1++) {
            //Target "inside" chunk - find *all* inside chunk possibilities
            for (int z = chunkMin.getFloorY() + 1; z < chunkMax.getFloorY(); z++) {
                int lv = z1 + 1;
                Log.info("Pass " + lv + " of " + chunkMax.getFloorY() + " - Z level " + z);
                ArrayList<Vector2d> rowChunks = new ArrayList<>();
                for (Vector2d chunkId : ownedChunks.keySet()) {
                    if (chunkId.getFloorY() != z) continue;
                    rowChunks.add(chunkId);
                }

                ArrayList<ArrayList<Vector2d>> rowSetData = this.findSets(rowChunks);

                //Test for closed iterating to top/bottom?

                //Assume two sets make the between an interior for now
                if (rowSetData.size() == 2) {
                    startingId = rowSetData.get(0).get(rowSetData.get(0).size() - 1).add(1, 0);
                }
            }

            Log.info("Found a starting ID at " + startingId);
            if (startingId != null) {
                buildInterior(startingId);
                ranInterior = true;
            }

            if (!ranInterior) break;
        }
    }

    private ArrayList<ArrayList<Vector2d>> findSets(ArrayList<Vector2d> rowChunks) {
        ArrayList<ArrayList<Vector2d>> chunkSets = new ArrayList<>();
        Collections.sort(rowChunks); //Ensure ids are X-> increasing
        Vector2d prevId = null;
        int i = 0;
        for (Vector2d id : rowChunks) {
            try {
                chunkSets.get(i);
            }
            catch (IndexOutOfBoundsException e) {
                chunkSets.add(new ArrayList<>());
            }

            if ((prevId == null)) {
                chunkSets.get(i).add(id);
                prevId = id;
                continue;
            }

            if (!isAdjacent(prevId, id)) {
                i++;
                chunkSets.add(new ArrayList<>());
                chunkSets.get(i).add(id);
            }
            else {
                chunkSets.get(i).add(id);
            }

            prevId = id;
        }

        return chunkSets;
    }

    private void buildInterior(Vector2d startingId) {
        if (startingId == null) return;
        if ((startingId.getFloorX() > maxChunk.getFloorX()) ||
            (startingId.getFloorX() < minChunk.getFloorX()) ||
            (startingId.getFloorY() > maxChunk.getFloorY()) ||
            (startingId.getFloorY() < minChunk.getFloorY())) return;

        if (!ownedChunks.containsKey(startingId)) {
            ZonedChunk newChunk = new ZonedChunk(startingId);
            newChunk.addOwner(this);
            ownedChunks.put(startingId, newChunk);

            //Use array iterative approach

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

        return (testX + 1 == prevX && testZ == prevZ) || (testX - 1 == prevX && testZ == prevZ);
    }
}
