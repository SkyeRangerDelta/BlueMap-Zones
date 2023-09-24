package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
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
    private static final HashMap<Player, String> playerLocations = new HashMap<>();
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
        ZonedChunk chunk = getChunk(chunkLocationID);

        if (chunk == null) {
            playerLocations.put(pc, "Wilderness");
            printNewLocation(pc, "Wilderness", false, chunkLocationID);
        }
        else {
            playerLocations.put(pc, chunk.getName());
            printNewLocation(pc, chunk.getName(), chunk.isConflicted(), chunkLocationID);
        }
    }

    private void isNewZone(Player pc, Vector2d chunkId) {
        String pcLastZone = playerLocations.get(pc);

        ZonedChunk chunk = getChunk(chunkId);

        if (chunk == null) {
            if (pcLastZone.equals("Wilderness")) return;
            playerLocations.put(pc, "Wilderness");
            printNewLocation(pc, "Wilderness", false, chunkId);
        }
        else {
            if (pcLastZone.equals(chunk.getName())) return;
            playerLocations.put(pc, chunk.getName());
            printNewLocation(pc, chunk.getName(), chunk.isConflicted(), chunkId);
        }
    }

    private boolean hasChangedChunks(Location oldLoc, Location newLoc) {
        return Math.floorDiv(newLoc.getBlockX(), 16) != Math.floorDiv(oldLoc.getBlockX(), 16) ||
                Math.floorDiv(newLoc.getBlockZ(), 16) != Math.floorDiv(oldLoc.getBlockZ(), 16);
    }

    public void setZonedShapes(ArrayList<ZonedShape> zonedShapes) {
        this.zonedShapes = zonedShapes;
    }

    private void printNewLocation(Player pc, String chunkName, boolean isBoundary, Vector2d chunkId) {

        Title newAreaTitle = Title.title(
                Component.text(chunkName),
                Component.text(buildSubtitle(chunkName))
        );

        Log.info("Player entered (" + chunkId.getX() + ", " + chunkId.getY() + ") - " + chunkName);

        pc.showTitle(newAreaTitle);
        pc.playSound(pc.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
    }

    private ZonedChunk getChunk(Vector2d chunkId) {
        for (ZonedShape zone : zonedShapes) {
            HashMap<Vector2d, ZonedChunk> ownedChunks = zone.getOwnedChunks();
            if (ownedChunks.containsKey(chunkId)) return ownedChunks.get(chunkId);
        }

        return null;
    }

    private String buildSubtitle(String mainTitle) {
        String subtitle = "";
        for (int i = 0; i < mainTitle.length() + 10; i++) {
            subtitle = subtitle.concat("_");
        }

        return subtitle;
    }
}
