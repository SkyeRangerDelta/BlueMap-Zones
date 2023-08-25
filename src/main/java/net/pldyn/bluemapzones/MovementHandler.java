package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class MovementHandler implements Listener {

    private static final Logger Log = Logger.getLogger("BM Zones");
    private static HashMap<Player, String> playerLocations = new HashMap<>();
    private ArrayList<ZonedShape> zonedShapes;

    public MovementHandler(ArrayList<ZonedShape> zonedShapes) {
        this.zonedShapes = zonedShapes;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (!e.hasChangedBlock()) return;

        Player pc = e.getPlayer();
        Location oldLocation = e.getFrom();
        Location newLocation = e.getTo();

        Vector2d newChunkId = new Vector2d(Math.floorDiv(newLocation.getBlockX(), 16),
                Math.floorDiv(newLocation.getBlockZ(), 16));

        if (!hasChangedChunks(oldLocation, newLocation)) return;

        isNewZone(pc, newChunkId);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player pc = e.getPlayer();
        Location pcLocation = pc.getLocation();
        Vector2d chunkLocationID = new Vector2d(Math.floorDiv(pcLocation.getBlockX(), 16),
                Math.floorDiv(pcLocation.getBlockZ(), 16));
        String chunkName = getChunkName(chunkLocationID);

        playerLocations.put(pc, chunkName);
        printNewLocation(pc, chunkLocationID, chunkName);
    }

    private void isNewZone(Player pc, Vector2d chunkId) {
        String pcLastZone = playerLocations.get(pc), newZone = "Wilderness";

        for (ZonedShape zone : zonedShapes) {
            if (zone.isOwnedChunk(chunkId)) {
                newZone = zone.getOwnedChunks().get(chunkId).getName();
            }
        }

        if (pcLastZone.equals(newZone)) return;

        playerLocations.put(pc, newZone);
        printNewLocation(pc, chunkId, newZone);
    }

    private boolean hasChangedChunks(Location oldLoc, Location newLoc) {
        return Math.floorDiv(newLoc.getBlockX(), 16) != Math.floorDiv(oldLoc.getBlockX(), 16) ||
                Math.floorDiv(newLoc.getBlockZ(), 16) != Math.floorDiv(oldLoc.getBlockZ(), 16);
    }

    public void setZonedShapes(ArrayList<ZonedShape> zonedShapes) {
        this.zonedShapes = zonedShapes;
    }

    private void printNewLocation(Player pc, Vector2d chunkID, String newZone) {
        Log.info("Player entered (" + chunkID.getX() + ", " + chunkID.getY() + ") - " + newZone);
        pc.sendMessage("Entered " + newZone);
    }

    private String getChunkName(Vector2d chunkId) {
        for (ZonedShape zone : zonedShapes) {
            HashMap<Vector2d, ZonedChunk> ownedChunks = zone.getOwnedChunks();
            if (ownedChunks.containsKey(chunkId)) return ownedChunks.get(chunkId).getName();
        }

        return "Wilderness";
    }
}
