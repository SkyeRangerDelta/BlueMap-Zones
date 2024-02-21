package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import it.unimi.dsi.fastutil.Hash;

import java.util.*;
import java.util.logging.Logger;

public class ZonedShape extends ShapeMarker {

    private static final Logger Log = Logger.getLogger("BM Zones");
    private HashMap<Vector2d, ZonedChunk> ownedChunks = new HashMap<>();
    public Vector2d maxChunk = getMaxChunk();
    public Vector2d minChunk = getMinChunk();

    /**
     * Constructor for a ZonedShape.
     * @param label The label of the shape.
     * @param shape The BlueMap shape type of the zone.
     * @param shapeY The Y level of the shape.
     */
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

    /**
     * Determines the maximum chunk ID (SE) of the zone shape.
     * @return The maximum chunk ID of the zone shape.
     */
    public Vector2d getMaxChunk() {
        Vector2d max = this.getShape().getMax();

        int maxX = Math.floorDiv(max.getFloorX(), 16);
        int maxZ = Math.floorDiv(max.getFloorY(), 16);

        return new Vector2d(maxX, maxZ);
    }

    /**
     * Determines the minimum chunk ID (NW) of the zone shape.
     * @return The minimum chunk ID of the zone shape.
     */
    public Vector2d getMinChunk() {
        Vector2d min = this.getShape().getMin();

        int minX = Math.floorDiv(min.getFloorX(), 16);
        int minZ = Math.floorDiv(min.getFloorY(), 16);

        return new Vector2d(minX, minZ);
    }

    /**
     * Finds all the chunks in a zone that have more than one owner.
     * @return A HashMap of Vector2d to ZonedChunk, of all conflicted chunks.
     */
    public HashMap<Vector2d, ZonedChunk> getConflictedChunks() {
        HashMap<Vector2d, ZonedChunk> conflictedChunks = new HashMap<>();
        for (ZonedChunk chunk : ownedChunks.values()) {
            if (chunk.isConflicted()) conflictedChunks.put(chunk.getChunkId(), chunk);
        }

        return conflictedChunks;
    }

    /**
     * Generates the interior of the zone shape.
     */
    public void doInteriorGeneration() {

        Log.info("Starting interior generation on " + this.getLabel());

        Vector2d chunkMax = this.getMaxChunk();
        Vector2d chunkMin = this.getMinChunk();

        //Determine sectors of the zone (8x8 set of chunks)
        int zoneWidth = chunkMax.getFloorX() - chunkMin.getFloorX();
        int zoneHeight = chunkMax.getFloorY() - chunkMin.getFloorY();

        int sectorWidth = Math.floorDiv(zoneWidth, 8); //Sector count width
        int sectorHeight = Math.floorDiv(zoneHeight, 8); //Sector count height

        Vector2d startingId = null;
        boolean ranInterior = false;

        //Run through each sector and recursively fill in the interior
        for (int sW = 0; sW < sectorWidth; sW++) { //Each sector across
            for (int sH = 0; sH < sectorHeight; sH++) {
                //Find a suitable 'interior' chunk to start from
            }
        }
    }

    /**
     * Finds sets of chunks in a row from the same zone.
     * @param rowChunks The chunks to find sets in.
     * @return An ArrayList of ArrayLists of Vector2d, each inner ArrayList being a set of chunks.
     */
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

    /**
     * Recursive method to build the interior of a zone shape.
     * @param startingId The starting chunk ID to build from.
     */
    private void buildInterior(Vector2d startingId) {
        Log.info("Starting interior build.");
        int queueSize = 1;
        Queue<Vector2d> chunkQueue = new LinkedList<>();
        chunkQueue.add(startingId);

        while (!chunkQueue.isEmpty()) {

            queueSize++;
            Vector2d currentId = chunkQueue.poll();

            if (ownedChunks.containsKey(currentId)) {
                continue;
            };

            Log.info("Adding chunk " + currentId + " to zone " + this.getLabel() + " interior.");

            ZonedChunk newChunk = new ZonedChunk(currentId);
            newChunk.addOwner(this);
            ownedChunks.put(currentId, newChunk);

            //Add valid adjacent chunks to the queue
        }

        Log.info("Finished with interior size " + queueSize + " chunks.");
    }

    /**
     * Determines if the testId is adjacent to the lastChunkId.
     * @param lastChunkId The last chunk ID to compare against.
     * @param testId The chunk ID to test.
     * @return True if the testId is adjacent to the lastChunkId, false otherwise.
     */
    private boolean isAdjacent(Vector2d lastChunkId, Vector2d testId) {
        //Check E, W, S, N (in order) for immediate or diagonal adjacency
        int prevX = lastChunkId.getFloorX();
        int prevZ = lastChunkId.getFloorY();
        int testX = testId.getFloorX();
        int testZ = testId.getFloorY();

        return (testX + 1 == prevX && testZ == prevZ) || (testX - 1 == prevX && testZ == prevZ);
    }

    private boolean isValidSurrounding(Vector2d testChunk) {
        boolean valid = false;
        boolean isOwned = ownedChunks.containsKey(testChunk);

        int[][] directions = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1},
                {-1, -1},
                {-1, 1},
                {1, -1},
                {1, 1}
        };

        return (valid && isOwned);
    }
}
