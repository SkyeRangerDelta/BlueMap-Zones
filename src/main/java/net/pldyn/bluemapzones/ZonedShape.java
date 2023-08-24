package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.ArrayList;
import java.util.logging.Logger;

public class ZonedShape extends ShapeMarker {

    private static final Logger Log = Logger.getLogger("BM Zones");
    private ArrayList<Vector2d> ownedChunks = new ArrayList<Vector2d>();

    public ZonedShape(String label, Shape shape, float shapeY) {
        super(label, shape, shapeY);
        Log.info("Created a new zone shape.");
    }

    public ArrayList<Vector2d> getOwnedChunks() {
        return ownedChunks;
    }

    public void setOwnedChunks(ArrayList<Vector2d> ownedChunks) {
        this.ownedChunks = ownedChunks;
    }

    public void addOwnedChunk(Vector2d newOwnedChunk) {
        this.ownedChunks.add(newOwnedChunk);
    }

    public boolean isOwnedChunk(Vector2d chunkID) {
        return ownedChunks.contains(chunkID);
    }
}
