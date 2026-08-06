package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.logging.Logger;

import static net.pldyn.bluemapzones.ConfigHandler.getNoticeType;

public class MovementHandler implements Listener {

  private static final Logger Log = Logger.getLogger("BM Zones");
  private static final HashMap<Player, PCLocationHistory> playerLocations = new HashMap<>();

  private TreeMap<Integer, ArrayList<ZonedShape>> zonesByLevel;

  private static String WILDERNESS = (String) ConfigHandler.getPluginConfFile().get( "Wilderness-Name" );

  public MovementHandler(TreeMap<Integer, ArrayList<ZonedShape>> zonesByLevel) {
    this.zonesByLevel = zonesByLevel;
  }

  public void setZonesByLevel(TreeMap<Integer, ArrayList<ZonedShape>> zonesByLevel) {
    this.zonesByLevel = zonesByLevel;
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent e) {
    if (!e.hasChangedBlock()) return;

    String worldName = (String) ConfigHandler.getPluginConfFile().get("Maps.name");

    Player pc = e.getPlayer();
    Location oldLocation = e.getFrom();
    Location newLocation = e.getTo();

    if (!hasChangedChunks(oldLocation, newLocation)) return;
    if (!newLocation.getWorld().getName().equals(worldName)) return;

    Vector2d newChunkId = toChunkId( newLocation );

    evaluateLocation( pc, newChunkId, true );
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e) {
    Player pc = e.getPlayer();

    // Seed the history from scratch, then announce where they landed.
    playerLocations.put( pc, new PCLocationHistory() );
    evaluateLocation( pc, toChunkId( pc.getLocation() ), true );
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent e) {
    // Without this the map holds a strong reference to every player who has ever
    // joined, for the lifetime of the server.
    playerLocations.remove( e.getPlayer() );
  }

  /**
   * @method evaluateLocation - Work out which zone the player occupies at every level,
   *     update their history, and announce if anything changed.
   * @param pc The player.
   * @param chunkId The chunk they are now in.
   * @param announce Whether a change should produce a notice.
   */
  private void evaluateLocation(Player pc, Vector2d chunkId, boolean announce) {
    PCLocationHistory history =
        playerLocations.computeIfAbsent( pc, key -> new PCLocationHistory() );

    boolean entered = false;
    boolean deepestConflicted = false;
    boolean occupiesAnything = false;

    boolean wasSomewhere = false;
    for (Integer level : zonesByLevel.keySet()) {
      if (history.getAreaName( level ) != null) {
        wasSomewhere = true;
        break;
      }
    }

    // Broadest level first, so the last occupied level seen is the deepest.
    for (Integer level : zonesByLevel.keySet()) {
      Resolution resolved = resolveLevel( level, chunkId );

      history.setAreaName( level, resolved.name );
      if (resolved.name != null) occupiesAnything = true;

      if (resolved.conflicted) {
        // Standing on a shared border. Leave the non-conflicted name alone so
        // stepping back into the zone we came from does not re-announce.
        deepestConflicted = true;
        continue;
      }

      deepestConflicted = false;

      boolean isNew = !Objects.equals( resolved.name, history.getNonConflictedAreaName( level ) );
      history.setNonConflictedAreaName( level, resolved.name );

      // Only *entering* somewhere is worth announcing. Walking out of a city back
      // into the state that contains it is not news, so a level going empty does
      // not fire on its own.
      if (isNew && resolved.name != null) entered = true;
    }

    // Leaving every zone at once is the exception: that genuinely is a new place.
    boolean leftEverything = !occupiesAnything && wasSomewhere;

    if (!announce || deepestConflicted) return;
    if (!entered && !leftEverything) return;

    sendNotice( pc, history );
  }

  /**
   * @method resolveLevel - Identify the zone a chunk belongs to within one level.
   *     Boundary chunks are looked up directly; interiors are resolved against the
   *     marker polygons. Levels never see each other's shapes.
   * @param level The zone level to search.
   * @param chunkId The chunk to identify.
   * @return The resolved name (null when in no zone) and whether it is a shared border.
   */
  private Resolution resolveLevel(int level, Vector2d chunkId) {
    ZonedChunk chunk = getChunk( level, chunkId );
    if (chunk != null) return new Resolution( chunk.getName(), chunk.isConflicted() );

    ZonedShape zone = findContainingShape( level, chunkId );
    if (zone != null) return new Resolution( zone.getLabel(), false );

    return new Resolution( null, false );
  }

  /**
   * @method findContainingShape - Find the shape at a level that contains a chunk,
   *     testing the marker polygons directly. When several contain it - a city inside a
   *     county sharing a level - the smallest wins, so the most specific zone is used.
   * @param level The zone level to search.
   * @param chunkId The chunk to identify.
   * @return The containing shape, or null.
   */
  private ZonedShape findContainingShape(int level, Vector2d chunkId) {
    // Test the middle of the chunk rather than a corner, so a chunk straddling an
    // edge resolves to whichever side holds most of it.
    double blockX = chunkId.getFloorX() * 16.0 + 8.0;
    double blockZ = chunkId.getFloorY() * 16.0 + 8.0;

    ZonedShape smallest = null;

    for (ZonedShape zone : zonesByLevel.getOrDefault( level, new ArrayList<>() )) {
      if (!zone.containsBlock( blockX, blockZ )) continue;
      if (smallest == null || zone.getBoundingArea() < smallest.getBoundingArea()) {
        smallest = zone;
      }
    }

    return smallest;
  }

  /**
   * @method sendNotice - Deliver the combined notice: the deepest zone the player is in,
   *     with the next level up as context.
   * @param pc The player.
   * @param history Their per-level location history.
   */
  private void sendNotice(Player pc, PCLocationHistory history) {
    List<String> occupied = new ArrayList<>();

    for (Integer level : zonesByLevel.keySet()) {
      String name = history.getAreaName( level );
      if (name != null) occupied.add( name );
    }

    String title = occupied.isEmpty() ? WILDERNESS : occupied.getLast();
    String context = occupied.size() > 1 ? occupied.get( occupied.size() - 2 ) : null;

    NoticeType noticeType = getNoticeType( pc.getUniqueId() );
    if (noticeType == NoticeType.OFF) return;

    if (noticeType.showsTitle()) {
      Title areaTitle = Title.title(
          Component.text( title ),
          Component.text( context != null ? context : buildSubtitle( title ) )
      );

      pc.showTitle( areaTitle );
      pc.playSound( pc.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f );
    }

    if (noticeType.showsChat()) {
      String line = context != null
          ? "Now entering " + title + ", " + context + "."
          : "Now entering " + title + ".";

      MessageHandler.send( pc, line, NamedTextColor.AQUA );
    }
  }

  /**
   * @method resolveAreaName - Public lookup used by the compass tool. Reports the
   *     deepest zone the chunk belongs to.
   * @param chunkId The chunk to identify.
   * @return The zone or boundary name, or the configured wilderness name.
   */
  public String resolveAreaName(Vector2d chunkId) {
    String deepest = null;

    for (Integer level : zonesByLevel.keySet()) {
      Resolution resolved = resolveLevel( level, chunkId );
      if (resolved.name != null) deepest = resolved.name;
    }

    return deepest != null ? deepest : WILDERNESS;
  }

  private Vector2d toChunkId(Location location) {
    return new Vector2d(Math.floorDiv(location.getBlockX(), 16),
        Math.floorDiv(location.getBlockZ(), 16));
  }

  private boolean hasChangedChunks(Location oldLoc, Location newLoc) {
    return Math.floorDiv(newLoc.getBlockX(), 16) != Math.floorDiv(oldLoc.getBlockX(), 16) ||
        Math.floorDiv(newLoc.getBlockZ(), 16) != Math.floorDiv(oldLoc.getBlockZ(), 16);
  }

  /**
   * @method getChunk - Look up a chunk within a single level.
   * @param level The zone level to search.
   * @param chunkId The chunk to find.
   * @return The owned chunk, or null if no shape at that level owns it.
   */
  private ZonedChunk getChunk(int level, Vector2d chunkId) {
    for (ZonedShape zone : zonesByLevel.getOrDefault( level, new ArrayList<>() )) {
      HashMap<Vector2d, ZonedChunk> ownedChunks = zone.getOwnedChunks();
      if (ownedChunks.containsKey(chunkId)) return ownedChunks.get(chunkId);
    }

    return null;
  }

  private String buildSubtitle(String mainTitle) {
    return "_".repeat( mainTitle.length() + 10 );
  }

  public void reloadConfig() {
    WILDERNESS = (String) ConfigHandler.getPluginConfFile().get( "Wilderness-Name" );
  }

  /** The outcome of identifying one chunk at one level. */
  private record Resolution(String name, boolean conflicted) { }
}
