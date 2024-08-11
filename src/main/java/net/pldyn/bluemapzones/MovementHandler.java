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
  private static final HashMap<Player, PCLocationHistory> playerLocations = new HashMap<>();
  private ArrayList<ZonedShape> zonedShapes;

  private static BlueMap_Zones BMZ = BlueMap_Zones.getInstance();

  private static final String WILDERNESS = BMZ.getConfig().getString("Wilderness-Name");

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

    ZonedChunk chunk = getChunk(chunkLocationID); //Test if the chunk is on a boundary
    ZonedShape zone = null;
    PCLocationHistory loginChunkData;

    if ( chunk == null ) { // If not, run bresenham
      zone = castRayInAllDirections( chunkLocationID );
      if ( zone != null ) {
        loginChunkData = new PCLocationHistory(
            zone.getLabel(), zone.getLabel()
        );
      }
      else {
        loginChunkData = new PCLocationHistory(
            WILDERNESS, WILDERNESS
        );
      }
    }
    else {
      String cName = chunk.getName();
      loginChunkData = new PCLocationHistory(
          cName, cName
      );
    }

    playerLocations.put(pc, loginChunkData);
    printNewLocation( pc, loginChunkData.getLastAreaName(), false, chunkLocationID );
  }

  private void isNewZone(Player pc, Vector2d chunkId, Vector2d lastChunkId) {
    PCLocationHistory pcHistory = playerLocations.get(pc);
    String pcLastZone = pcHistory.getLastAreaName();
    String pcLastNonConflictedZone = pcHistory.getLastNonConflictedAreaName();

    ZonedChunk chunk = getChunk(chunkId); //Determine if the chunk is owned by a zone
    ZonedChunk lastChunk = getChunk(lastChunkId); //Determine if the last chunk is owned by a zone

    if (chunk == null) { // Not a boundary chunk (must be a ext or int chunk)
      // Run bresenham to determine if inside a shape
      ZonedShape zone = castRayInAllDirections(chunkId);

      if (zone == null) {
        if (pcLastZone.equals(WILDERNESS)) return;
        pcHistory.setLastAreaName(WILDERNESS);
        printNewLocation(pc, WILDERNESS, false, chunkId);
      }
      else { // Inside a detected zone, therefore not a border - must be interior
        Log.info( "Zone: " + zone.getLabel() );
        if (pcLastZone.equals(zone.getLabel())) return;
        pcHistory.setLastAreaName(zone.getLabel());

        // Check if the last zone was null or not. This determines if the last zone was a border zone or
        // if they've warped in.
        if ( lastChunk == null ) {
          // Warped or joined oddly.
          pcHistory.setLastNonConflictedAreaName(zone.getLabel());
          printNewLocation( pc, zone.getLabel(), false, chunkId );
        }
        else {
          // Last chunk is known. If it was owned by the same zone, return.
          if ( lastChunk.getOwners().contains( zone ) && !lastChunk.isConflicted() ) return;

          // If the last chunk was a border chunk and the last non-conflicted chunk they were in
          // was an owner in the border chunk, return.
          if ( lastChunk.isConflicted() && Objects.equals( zone.getLabel(), pcLastNonConflictedZone ) ) return;

          // Otherwise, print the new location.
          printNewLocation( pc, zone.getLabel(), false, chunkId );
        }

        pcHistory.setLastNonConflictedAreaName(zone.getLabel());
      }
    }
    else { // Owned by a zone (border chunk)
      if (pcLastZone.equals(chunk.getName())) return;
      pcHistory.setLastAreaName(chunk.getName());

      if ( !chunk.isConflicted() ) {
        pcHistory.setLastNonConflictedAreaName(chunk.getName());
      }

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
