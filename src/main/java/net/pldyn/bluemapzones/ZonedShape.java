package net.pldyn.bluemapzones;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.*;
import java.util.logging.Logger;

public class ZonedShape extends ShapeMarker {

  private static final Logger Log = Logger.getLogger("BM Zones");
  private HashMap<Vector2d, ZonedChunk> ownedChunks = new HashMap<>();
  public Vector2d maxChunk = getMaxChunk();
  public Vector2d minChunk = getMinChunk();

  /**
   * Constructor for a ZonedShape.
   * @param label The label of the shape.
   * @param shape The BlueMap shape type of the zone.
   * @param shapeY The Y level of the shape.
   */
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

  /**
   * Determines the maximum chunk ID (SE) of the zone shape.
   * @return The maximum chunk ID of the zone shape.
   */
  public Vector2d getMaxChunk() {
    Vector2d max = this.getShape().getMax();

    int maxX = Math.floorDiv(max.getFloorX(), 16);
    int maxZ = Math.floorDiv(max.getFloorY(), 16);

    return new Vector2d(maxX, maxZ);
  }

  /**
   * Determines the minimum chunk ID (NW) of the zone shape.
   * @return The minimum chunk ID of the zone shape.
   */
  public Vector2d getMinChunk() {
    Vector2d min = this.getShape().getMin();

    int minX = Math.floorDiv(min.getFloorX(), 16);
    int minZ = Math.floorDiv(min.getFloorY(), 16);

    return new Vector2d(minX, minZ);
  }

  /**
   * Finds all the chunks in a zone that have more than one owner.
   * @return A HashMap of Vector2d to ZonedChunk, of all conflicted chunks.
   */
  public HashMap<Vector2d, ZonedChunk> getConflictedChunks() {
    HashMap<Vector2d, ZonedChunk> conflictedChunks = new HashMap<>();
    for (ZonedChunk chunk : ownedChunks.values()) {
      if (chunk.isConflicted()) conflictedChunks.put(chunk.getChunkId(), chunk);
    }

    return conflictedChunks;
  }

  /**
   * Generates the interior of the zone shape.
   */
  public void doInteriorGeneration() {

    int intCount = 0;
    Log.info( "Starting interior generation on " + this.getLabel() );

    // Start by finding an interior chunk. We'll do this by identifying a chunk ID that has a boundary chunk set in each cardinal direction.
    // We'll start by moving tile by tile from min to max chunks checking for neighbors.
    // When one is found, we'll execute an iterative flood fill to build the interior.
    // Then run the same process to verify no chunks are missed.

    for (int x = minChunk.getFloorX(); x <= maxChunk.getFloorX(); x++) {
      for (int z = minChunk.getFloorY(); z <= maxChunk.getFloorY(); z++) {
        if (ownedChunks.containsKey(new Vector2d(x, z))) {
          // Skip if we already know about this one
          continue;
        }

        Vector2d startingId = new Vector2d(x, z);
        Vector2d internalId = isInternal(startingId);

        if (internalId != null) {
          intCount++;
          Log.info( "Found an internal (" + intCount + ") chunk at " + internalId + " for zone " + this.getLabel() );
          buildInterior(internalId);
        }
      }
    }

  }

  /**
   * @method isInternal - Casts a ray to all cardinal directions to determine if the chunk is internal.
   * @param startingId - The starting chunk ID to check.
   */
  private Vector2d isInternal(Vector2d startingId) {
    Log.info( "Checking " + startingId );
    boolean isInternal = true;
    Vector2d testId = null;

    int[][] directions = {
        { 1, 0 },
        { - 1, 0 },
        { 0, 1 },
        { 0, - 1 }
    };

    for ( int[] direction : directions ) {
      testId = new Vector2d( startingId.getFloorX() + direction[ 0 ], startingId.getFloorY() + direction[ 1 ] );
      if ( ! ownedChunks.containsKey( testId ) ) {
        isInternal = false;
        break;
      }
    }

    if ( isInternal ) {
      Log.info( "Chunk " + startingId + " is internal." );
      return testId;
    } else {
      return null;
    }
  }

  /**
   * Recursive method to build the interior of a zone shape.
   * @param startingId The starting chunk ID to build from.
   */
  private void buildInterior(Vector2d startingId) {
    Log.info("Starting interior build.");
    Queue<Vector2d> chunkQueue = new LinkedList<>();
    chunkQueue.add(startingId);

    int addedChunks = 0;

    while (!chunkQueue.isEmpty()) {
      Vector2d currentId = chunkQueue.poll();

      if (ownedChunks.containsKey(currentId)) {
        continue;
      };

      Log.info("Adding chunk " + currentId + " to zone " + this.getLabel() + " interior.");

      ZonedChunk newChunk = new ZonedChunk(currentId);
      newChunk.addOwner(this);
      ownedChunks.put(currentId, newChunk);
      addedChunks++;

      //Add valid adjacent chunks to the queue
      int[][] directions = {
          { 1, 0 },
          { - 1, 0 },
          { 0, 1 },
          { 0, - 1 }
      };

      for ( int[] direction : directions ) {
        Vector2d intId = new Vector2d( startingId.getFloorX() + direction[ 0 ], startingId.getFloorY() + direction[ 1 ] );
        if ( !ownedChunks.containsKey( intId ) && !chunkQueue.contains( intId ) ) {
          chunkQueue.add( intId );
        }
      }
    }

    Log.info("Finished with interior size " + addedChunks + " chunks.");
  }

  /**
   * Determines if the testId is adjacent to the lastChunkId.
   * @param lastChunkId The last chunk ID to compare against.
   * @param testId The chunk ID to test.
   * @return True if the testId is adjacent to the lastChunkId, false otherwise.
   */
  private boolean isAdjacent(Vector2d lastChunkId, Vector2d testId) {
    //Check E, W, S, N (in order) for immediate or diagonal adjacency
    int prevX = lastChunkId.getFloorX();
    int prevZ = lastChunkId.getFloorY();
    int testX = testId.getFloorX();
    int testZ = testId.getFloorY();

    return (testX + 1 == prevX && testZ == prevZ) || (testX - 1 == prevX && testZ == prevZ);
  }

  private boolean isValidSurrounding(Vector2d testChunk) {
    boolean valid = false;
    boolean isOwned = ownedChunks.containsKey(testChunk);

    int[][] directions = {
        {1, 0},
        {-1, 0},
        {0, 1},
        {0, -1},
        {-1, -1},
        {-1, 1},
        {1, -1},
        {1, 1}
    };

    return (valid && isOwned);
  }
}
