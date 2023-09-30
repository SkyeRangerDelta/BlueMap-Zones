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

    public void buildInterior(Vector2d startingId) {
        if (!ownedChunks.containsKey(startingId)) {
            ZonedChunk newChunk = new ZonedChunk(startingId);
            newChunk.addOwner(this);
            ownedChunks.put(startingId, newChunk);

            buildInterior(startingId.add(1, 0)); //E
            buildInterior(startingId.sub(1, 0)); //W
            buildInterior(startingId.add(0, 1)); //S
            buildInterior(startingId.sub(0, 1)); //N
        }
    }
}
