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
}
