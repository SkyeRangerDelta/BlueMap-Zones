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

import java.util.*;
import java.util.logging.Logger;

import static net.pldyn.bluemapzones.ConfigHandler.getNoticeExclusions;

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

    String worldName = (String) ConfigHandler.getPluginConfFile().get("Maps.name");

    Player pc = e.getPlayer();
    Location oldLocation = e.getFrom();
    Location newLocation = e.getTo();

    Vector2d newChunkId = new Vector2d(Math.floorDiv(newLocation.getBlockX(), 16),
        Math.floorDiv(newLocation.getBlockZ(), 16));

    Vector2d oldChunkId = new Vector2d(Math.floorDiv(oldLocation.getBlockX(), 16),
        Math.floorDiv(oldLocation.getBlockZ(), 16));

    if (!hasChangedChunks(oldLocation, newLocation)) return;
    if (!newLocation.getWorld().getName().equals(worldName)) return;

    isNewZone( pc, newChunkId, oldChunkId );
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
      isNewZone( pc, chunkLocationID, chunkLocationID );
    }
    else {
      playerLocations.put(pc, chunk.getName());
      printNewLocation(pc, chunk.getName(), chunk.isConflicted(), chunkLocationID);
    }
  }

  private void isNewZone(Player pc, Vector2d chunkId, Vector2d lastChunkId) {
    String pcLastZone = playerLocations.get(pc);

    ZonedChunk chunk = getChunk(chunkId);
    ZonedChunk lastChunk = getChunk(lastChunkId);

    //TODO: If leaving boundary chunk; check if new owner was an owner on the last chunk and do nothing

//    if ( lastChunk.isBoundary() ) {
//      List<ZonedShape> newOwners = chunk.getOwners();
//      List<ZonedShape> lastOwners = lastChunk.getOwners();
//
//      for ( ZonedShape newOwner : newOwners ) {
//        if ( !lastOwners.contains( newOwner ) ) {
//          playerLocations.put(pc, newOwner.getLabel());
//          printNewLocation(pc, newOwner.getLabel(), false, chunkId);
//          return;
//        }
//      }
//    }

    if (chunk == null) {
      // Run bresenham to determine if inside a shape
      ZonedShape zone = castRayInAllDirections(chunkId);
      Log.info( "Zone: " + zone );

      if (zone == null) {
        if (pcLastZone.equals("Wilderness")) return;
        playerLocations.put(pc, "Wilderness");
        printNewLocation(pc, "Wilderness", false, chunkId);
      }
      else {
        if (pcLastZone.equals(zone.getLabel())) return;
        playerLocations.put(pc, zone.getLabel());
        printNewLocation(pc, zone.getLabel(), false, chunkId);
      }
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


  /**
   * @method runBresenham - Runs the Bresenham algorithm to determine the line between two points.
   * @param start - The starting point.
   * @param end - The ending point.
   * @return ArrayList<Vector2d> - The list of chunk IDs between the two points.
   */
  private ArrayList<Vector2d> runBresenham(Vector2d start, Vector2d end) {
    ArrayList<Vector2d> lineIds = new ArrayList<>();

    int x1 = start.getFloorX();
    int x2 = end.getFloorX();
    int y1 = start.getFloorY();
    int y2 = end.getFloorY();

    int dx = Math.abs(x2 - x1);
    int dy = Math.abs(y2 - y1);

    int sx = Integer.signum(x2 - x1);
    int sy = Integer.signum(y2 - y1);

    boolean swap = dy > dx;
    if (swap) {
      int temp = dx;
      dx = dy;
      dy = temp;
    }

    int error = 2 * dy - dx;

    int x = x1;
    int y = y1;

    for (int i = 0; i <= dx; i++) {
      Vector2d currentId = new Vector2d(x, y);
      lineIds.add(new Vector2d(x, y));

      ZonedChunk chunk = getChunk(currentId);
      if (chunk != null) {
        return lineIds;
      }

      while (error >= 0) {
        if (swap) {
          x += sx;
        } else {
          y += sy;
        }
        error -= 2 * dx;
      }

      if (swap) {
        y += sy;
      } else {
        x += sx;
      }
      error += 2 * dy;
    }

    return lineIds;
  }

  /**
   * @method castRayInAllDirections - Dispatches the ray in all directions and returns the zone if found.
   * @param playerLocation - the chunk ID of where the player is.
   * @return ZonedShape - the zone the player is in if any.
   */
  private ZonedShape castRayInAllDirections(Vector2d playerLocation) {
    ArrayList< ZonedShape > zones = new ArrayList<>();
    HashMap< ZonedShape, Integer > zoneCount = new HashMap<>();

    Log.info( "Casting ray in all directions from " + playerLocation );

    // Define the directions
    Vector2d[] directions = {
        new Vector2d( 0, 1 ),   // North
        new Vector2d( 0, - 1 ),  // South
        new Vector2d( 1, 0 ),   // East
        new Vector2d( - 1, 0 )   // West
    };

    for ( Vector2d direction : directions ) {
      Vector2d endPoint = playerLocation.add( direction.mul( 250 ) );
      Log.info( "Casting ray to " + endPoint );
      ArrayList< Vector2d > rayCastResult = runBresenham( playerLocation, endPoint );

      for ( Vector2d chunkId : rayCastResult ) {
        ZonedChunk chunk = getChunk( chunkId );
        if ( chunk != null ) {
          List<ZonedShape> owners = chunk.getOwners();
          for ( ZonedShape zone : owners ) {
            if ( zone != null ) {
              zones.add(zone);
              zoneCount.put(zone, zoneCount.getOrDefault(zone, 0) + 1);
            }
          }
        }
      }
    }

    // Check if any two zones are the same
    for ( Map.Entry<ZonedShape, Integer> entry : zoneCount.entrySet()) {
      if (entry.getValue() >= 4) {
        return entry.getKey();
      }
    }

    return null;
  }

  public void setZonedShapes(ArrayList<ZonedShape> zonedShapes) {
    this.zonedShapes = zonedShapes;
  }

  private void printNewLocation(Player pc, String chunkName, boolean isBoundary, Vector2d chunkId) {

    if ( isBoundary ) return;

    Title newAreaTitle = Title.title(
        Component.text(chunkName),
        Component.text(buildSubtitle(chunkName))
    );

    Log.info("Player entered (" + chunkId.getX() + ", " + chunkId.getY() + ") - " + chunkName);

    List<UUID> exclusionsList = getNoticeExclusions();
    if (exclusionsList.contains(pc.getUniqueId())) return;

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
