package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.ArrayList;

public class ShapedChunk {
    private ShapeMarker shape;
    private ArrayList<Vector2d> chunks;
    public ShapedChunk(ShapeMarker shape) {
        this.shape = shape;
        this.chunks = new ArrayList<Vector2d>();
    }

    public ShapeMarker getShape() {
        return shape;
    }

    public void addChunk(Vector2d chunkID) {
        chunks.add(chunkID);
    }

    public ArrayList<Vector2d> getChunks() {
        return chunks;
    }

    public void setChunks(ArrayList<Vector2d> chunks) {
        this.chunks = chunks;
    }

    public void setShape(ShapeMarker shape) {
        this.shape = shape;
    }
}
