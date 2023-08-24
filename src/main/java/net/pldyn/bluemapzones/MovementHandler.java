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

        Vector2d newChunkLocation = new Vector2d(Math.floorDiv(newLocation.getBlockX(), 16),
                Math.floorDiv(newLocation.getBlockZ(), 16));

        if (!hasChangedChunks(oldLocation, newLocation)) return;

        isNewZone(pc, newChunkLocation);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player pc = e.getPlayer();
        Location pcLocation = pc.getLocation();
        Vector2d chunkLocation = new Vector2d(Math.floorDiv(pcLocation.getBlockX(), 16),
                Math.floorDiv(pcLocation.getBlockZ(), 16));

        playerLocations.put(pc, getOwnedChunkName(chunkLocation));
        printNewLocation(pc, chunkLocation);
    }

    private void isNewZone(Player pc, Vector2d newChunkLocation) {
        String pcLastZone = playerLocations.get(pc), newZone = "Wilderness";

        for (ZonedShape zone : zonedShapes) {
            if (zone.isOwnedChunk(newChunkLocation)) {
                newZone = zone.getLabel();
            }
        }

        if (pcLastZone.equals(newZone)) return;

        playerLocations.put(pc, newZone);
        printNewLocation(pc, newChunkLocation);
    }

    private boolean hasChangedChunks(Location oldLoc, Location newLoc) {
        return Math.floorDiv(newLoc.getBlockX(), 16) != Math.floorDiv(oldLoc.getBlockX(), 16) ||
                Math.floorDiv(newLoc.getBlockZ(), 16) != Math.floorDiv(oldLoc.getBlockZ(), 16);
    }

    public void setZonedShapes(ArrayList<ZonedShape> zonedShapes) {
        this.zonedShapes = zonedShapes;
    }

    private void printNewLocation(Player pc, Vector2d chunkID) {
        String newLocArea = playerLocations.get(pc);

        Log.info("Player entered (" + chunkID.getX() + ", " + chunkID.getY() + ") - " + newLocArea);
        pc.sendMessage("Entered " + newLocArea);
    }

    private String getOwnedChunkName(Vector2d chunkId) {
        for (ZonedShape zone : zonedShapes) {
            if (zone.isOwnedChunk(chunkId)) return zone.getLabel();
        }

        return "Wilderness";
    }
}
