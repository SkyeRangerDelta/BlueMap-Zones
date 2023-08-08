package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

public class ShapedChunk implements Listener {
    private ShapeMarker shape;
    private ArrayList<Vector2d> chunks;
    private static final Logger Log = Logger.getLogger("BM Zones");
    public ShapedChunk(ShapeMarker shape) {
        this.shape = shape;
        this.chunks = new ArrayList<Vector2d>();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Location newLoc = event.getTo();
        Location oldLoc = event.getFrom();

        if (changedChunks(newLoc, oldLoc)) {
            Log.info("Player changed cells.");

            if (determineRelevantChunk(newLoc, oldLoc)) {
                Player ePlayer = event.getPlayer();
                Log.info("Player walked into a shaped chunk.");
                ePlayer.sendMessage("Walked into a shaped chunk.");
            }
        }
    }

    private boolean changedChunks(Location newLocation, Location oldLocation) {
        return Math.floorDiv(newLocation.getBlockX(), 16) != Math.floorDiv(oldLocation.getBlockX(), 16) ||
                Math.floorDiv(newLocation.getBlockZ(), 16) != Math.floorDiv(oldLocation.getBlockZ(), 16);
    }

    private Vector2d toChunkID(int xCoord, int zCoord) {
        int chunkX = Math.floorDiv(xCoord, 16);
        int chunkZ = Math.floorDiv(zCoord, 16);
        return new Vector2d(chunkX, chunkZ);
    }

    private boolean determineRelevantChunk(Location newLoc, Location oldLoc) {
        Vector2d chunkID = toChunkID(newLoc.getBlockX(), newLoc.getBlockZ());
        Log.info("Testing chunk " + chunkID.getX() + ", " + chunkID.getY());
        return chunks.contains(chunkID);
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
